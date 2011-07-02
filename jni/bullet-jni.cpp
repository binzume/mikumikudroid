#include <string.h>
#include <jni.h>

#include <btBulletDynamicsCommon.h>
#include <btBulletCollisionCommon.h>

btDiscreteDynamicsWorld *mDynamicsWorld;
btRigidBody *rb[4096];
btDefaultMotionState *ms[4096];
int g_ptr;

btGeneric6DofSpringConstraint *g_cst[4096];
int g_cptr;

extern "C" void Java_jp_gauzau_MikuMikuDroid_Miku_initFaceNative(JNIEnv* env, jobject thiz, jobject vertex, jint count, jobject index, jobject offset)
{
	float* vert = (float*)env->GetDirectBufferAddress(vertex);
	int*   idx  = (int*)  env->GetDirectBufferAddress(index);
	float* ofs  = (float*)env->GetDirectBufferAddress(offset);
	
	for(int i = 0; i < count; i++) {
		for(int j = 0; j < 3; j++) {
			vert[idx[i] * 8 + j] = ofs[i * 3 + j];
		}
	}
	
	return ;
}

extern "C" void Java_jp_gauzau_MikuMikuDroid_Miku_setFaceNative(JNIEnv* env, jobject thiz, jobject vertex, jobject pointer, jint count, jobject index, jobject offset, jfloat weight)
{
	float* vert = (float*)env->GetDirectBufferAddress(vertex);
	int*   ptr  = (int*)  env->GetDirectBufferAddress(pointer);
	int*   idx  = (int*)  env->GetDirectBufferAddress(index);
	float* ofs  = (float*)env->GetDirectBufferAddress(offset);
	
	for(int i = 0; i < count; i++) {
		for(int j = 0; j < 3; j++) {
			vert[ptr[idx[i]] * 8 + j] += ofs[i * 3 + j] * weight;
		}
	}
	
	return ;
}

extern "C" void Java_jp_gauzau_MikuMikuDroid_CoreLogic_btMakeWorld(JNIEnv* env, jobject thiz)
{
	// make world
	btCollisionConfiguration *collisionConfiguration = new btDefaultCollisionConfiguration();
	btCollisionDispatcher *dispatcher = new btCollisionDispatcher(collisionConfiguration);
	btDbvtBroadphase *broadphase = new btDbvtBroadphase();
	btSequentialImpulseConstraintSolver *solver = new btSequentialImpulseConstraintSolver();
	mDynamicsWorld = new btDiscreteDynamicsWorld(dispatcher, broadphase, solver, collisionConfiguration);

	btVector3 *g = new btVector3(0, -9.8, 0);
	mDynamicsWorld->setGravity(*g);
	
	g_ptr = 0;
	g_cptr = 0;
}

btVector3 createBtVector3(JNIEnv* env, jfloatArray vec)
{
	float* vec_n = env->GetFloatArrayElements(vec, 0);
	float x = vec_n[0];
	float y = vec_n[1];
	float z = vec_n[2];
	env->ReleaseFloatArrayElements(vec, vec_n, 0);
	
	return btVector3(x, y, z);
}

btMatrix3x3 createBtMatrix3x3(JNIEnv* env, jfloatArray rot)
{
	float* rot_n = env->GetFloatArrayElements(rot, 0);
	float rx = rot_n[0];
	float ry = rot_n[1];
	float rz = rot_n[2];
	
	btMatrix3x3 mat;
	mat.setIdentity();
	mat.setEulerZYX(rx, ry, rz);
	
	env->ReleaseFloatArrayElements(rot, rot_n, 0);	
	
	return mat;
}

btTransform createBtTransform(JNIEnv* env, jfloatArray pos, jfloatArray rot)
{
	return btTransform(createBtMatrix3x3(env, rot), createBtVector3(env, pos));
}


extern "C" jint Java_jp_gauzau_MikuMikuDroid_Miku_btAddRigidBody(JNIEnv* env, jobject thiz,
		jint type, jint shape,
		jfloat w, jfloat h, jfloat d,
		jfloatArray pos, jfloatArray rot, 
		jfloat mass, jfloat v_dim, jfloat r_dim, jfloat recoil, jfloat friction,
		jbyte group_index, jshort group_target)
{
	// create CollisionShape
	btCollisionShape* cs;
	switch(shape) {
	case 0: //sphere
		cs = new btSphereShape(w);
		break;
	case 1: // box
		cs = new btBoxShape(btVector3(w, h, d));
		break;
	case 2: // capsule
		cs = new btCapsuleShape(w, h);
		break;
	default:
		cs = 0;	// NullPointerException
		break;
	}

	// position and rotation
	btTransform transf = createBtTransform(env, pos, rot);

	// inertia
	btVector3 inertia(0, 0, 0);
	cs->calculateLocalInertia(type == 0 ? 0 : mass, inertia);
	
	// create rigid body with default motion state
	ms[g_ptr] = new btDefaultMotionState(transf);
	
	btRigidBody :: btRigidBodyConstructionInfo* rbi = new btRigidBody :: btRigidBodyConstructionInfo(type == 0 ? 0 : mass, ms[g_ptr], cs, inertia);
	rbi->m_linearDamping = v_dim;
	rbi->m_angularDamping = r_dim;
	rbi->m_restitution = recoil;
	rbi->m_friction = friction;
	rb[g_ptr] = new btRigidBody(*rbi);
	
	if(type == 0) {
		rb[g_ptr]->setActivationState(DISABLE_DEACTIVATION);
		rb[g_ptr]->setCollisionFlags(rb[g_ptr]->getCollisionFlags() | btCollisionObject :: CF_KINEMATIC_OBJECT);
	}

	mDynamicsWorld->addRigidBody(rb[g_ptr], (1 << group_index), group_target);
	
	return g_ptr++;
}

extern "C" jint Java_jp_gauzau_MikuMikuDroid_Miku_btAddJoint(JNIEnv* env, jobject thiz,
			jint rb1, jint rb2, jfloatArray pos, jfloatArray rot, jfloatArray p1, jfloatArray p2, jfloatArray r1, jfloatArray r2, jfloatArray sp, jfloatArray sr)
{
	btTransform jt = createBtTransform(env, pos, rot);
	
	btTransform tr1 = rb[rb1]->getCenterOfMassTransform().inverse() * jt;
	btTransform tr2 = rb[rb2]->getCenterOfMassTransform().inverse() * jt;
	btGeneric6DofSpringConstraint* dof = new btGeneric6DofSpringConstraint(*rb[rb1], *rb[rb2], tr1, tr2, true);
	
	dof->setLinearLowerLimit(createBtVector3(env, p1));
	dof->setLinearUpperLimit(createBtVector3(env, p2));
	dof->setAngularLowerLimit(createBtVector3(env, r1));
	dof->setAngularUpperLimit(createBtVector3(env, r2));
	
	float *sp_n = env->GetFloatArrayElements(sp, 0);
	float *sr_n = env->GetFloatArrayElements(sr, 0);	
	for(int i = 0; i < 3; i++) {
		dof->enableSpring(i, true);
		dof->setStiffness(i, sp_n[i]);

		dof->enableSpring(i + 3, true);
		dof->setStiffness(i + 3, sr_n[i]);
	}
	env->ReleaseFloatArrayElements(sp, sp_n, 0);
	env->ReleaseFloatArrayElements(sr, sr_n, 0);
	
	mDynamicsWorld->addConstraint(dof, true);	// disableCollisionsBetweenLinkedBodies
	g_cst[g_cptr] = dof;
	
	return g_cptr++;
}

extern "C" void Java_jp_gauzau_MikuMikuDroid_CoreLogic_btClearAllData(JNIEnv* env, jobject thiz)
{
	for(int i = 0; i < g_cptr; i++) {
		mDynamicsWorld->removeConstraint(g_cst[i]);
		delete g_cst[i];
		g_cst[i] = 0;
	}
	
	for(int i = 0; i < g_ptr; i++) {
		mDynamicsWorld->removeRigidBody(rb[i]);
		delete rb[i];
		delete ms[i];
		rb[i] = 0;
		ms[i] = 0;
	}
	
	g_ptr = 0;
	g_cptr = 0;
}

extern "C" void Java_jp_gauzau_MikuMikuDroid_CoreLogic_btStepSimulation(JNIEnv* env, jobject thiz, jfloat step)
{
	mDynamicsWorld->stepSimulation(step, 4);
//	mDynamicsWorld->stepSimulation(step, 0);
}

extern "C" void Java_jp_gauzau_MikuMikuDroid_Miku_btGetOpenGLMatrix(JNIEnv* env, jobject thiz, jint rb,	jfloatArray matrix, jfloatArray pos, jfloatArray rot)
{
	// rigid body initial position & rotation
	btTransform rbt = createBtTransform(env, pos, rot);

	// rigid body in dynamics world
	btTransform tr;
	ms[rb]->getWorldTransform(tr);

	tr = tr * rbt.inverse();

	float* matrix_native = env->GetFloatArrayElements(matrix, 0);
	tr.getOpenGLMatrix(matrix_native);
	env->ReleaseFloatArrayElements(matrix, matrix_native, 0);
}

extern "C" void Java_jp_gauzau_MikuMikuDroid_Miku_btSetOpenGLMatrix(JNIEnv* env, jobject thiz, jint rb, jfloatArray matrix, jfloatArray pos, jfloatArray rot)
{
	if(rb < g_ptr) {
		// rigid body initial position & rotation
		btTransform rbt = createBtTransform(env, pos, rot);
	
		// rigid body in VMD world
		float* matrix_native = env->GetFloatArrayElements(matrix, 0);
		btTransform tr;
		tr.setFromOpenGLMatrix(matrix_native);
		env->ReleaseFloatArrayElements(matrix, matrix_native, 0);

		tr = tr * rbt;	
	
		ms[rb]->setWorldTransform(tr);
	}
}

extern "C" void Java_jp_gauzau_MikuMikuDroid_Miku_btForceOpenGLMatrix(JNIEnv* env, jobject thiz, jint rbi, jfloatArray matrix, jfloatArray pos, jfloatArray rot)
{
	// rigid body initial position & rotation
	btTransform rbt = createBtTransform(env, pos, rot);
	
	// rigid body in VMD world
	float* matrix_native = env->GetFloatArrayElements(matrix, 0);
	btTransform tr;
	tr.setFromOpenGLMatrix(matrix_native);
	env->ReleaseFloatArrayElements(matrix, matrix_native, 0);

	tr = tr * rbt;	
	
	btVector3 zero(0, 0, 0);
	ms[rbi]->setWorldTransform(tr);
	rb[rbi]->setLinearVelocity(zero);
	rb[rbi]->setAngularVelocity(zero);
	rb[rbi]->setCenterOfMassTransform(tr);
}