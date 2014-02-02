package jp.gauzau.MikuMikuDroid;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import android.util.Log;

public class PMXParser extends ParserBase implements ModelFile {
	public static final int ATTR_STRING_ENCODING = 0;
	public static final int ATTR_OPTION_UV = 1;
	public static final int ATTR_VERT_INDEX_SZ = 2;
	public static final int ATTR_TEXTURE_INDEX_SZ = 3;
	public static final int ATTR_MATERIAL_INDEX_SZ = 4;
	public static final int ATTR_BONE_INDEX_SZ = 5;
	public static final int ATTR_MORPH_INDEX_SZ = 6;
	public static final int ATTR_RIGIDBODY_INDEX_SZ = 7;

	private String mFileName;
	private boolean mIsPmd;
	private String mModelName;
	private String mDescription;
	private ArrayList<Material> mMaterial;
	private ArrayList<Bone> mBone;
	private ArrayList<IK> mIK;
	private ArrayList<Face> mFace;
	private ArrayList<Short> mSkinDisp;
	private ArrayList<String> mBoneDispName;
	private ArrayList<BoneDisp> mBoneDisp;
	private byte mHasEnglishName;
	private String mEnglishModelName;
	private String mEnglishComment;
	private ArrayList<String> mEnglishBoneName;
	private ArrayList<String> mEnglishSkinName;
	private ArrayList<String> mToonFileName;
	private ArrayList<String> mEnglishBoneDispName;
	private ArrayList<RigidBody> mRigidBody;
	private ArrayList<Joint> mJoint;
	private boolean mIsOneSkinning = true;

	public ShortBuffer mIndexBuffer;
	public FloatBuffer mVertBuffer;
	public ShortBuffer mWeightBuffer;

	private int mVertexPos;
	private int[] mInvMap;
	
	private byte[] options;

	public PMXParser(String base, String file) throws IOException {
		super(file);
		mFileName = file;
		mIsPmd = false;
		File f = new File(file);
		String path = f.getParent() + "/";

		try {
			options = parsePMXHeader();
			if (mIsPmd) {
				parsePMDVertexList();
				parsePMDIndexList();
				parsePMDMaterialList(path);
				parsePMDBoneList();
				parsePMDFaceList();
				parsePMDSkinDisp();
				if (!isEof()) {
					parsePMDRigidBody();
					parsePMDJoint();
					mToonFileName = new ArrayList<String>(11);
					mToonFileName.add(0, base + "Data/toon0.bmp");
					for (int i = 0; i < 10; i++) {
						String str = String.format(base + "Data/toon%02d.bmp", i + 1);
						mToonFileName.add(i + 1, str);
					}

				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			mIsPmd = false;
		}
	}

	private void parsePMDJoint() {
		int num = getInt();
		Log.d("PMDParser", "Joint: " + String.valueOf(num));
		if (num > 0) {
			mJoint = new ArrayList<Joint>(num);
			for (int i = 0; i < num; i++) {
				Joint j = new Joint();
				j.name = readTexBuf();
				readTexBuf();
				getByte(); // type (always zero)
				j.rigidbody_a = readRigidIndex();
				j.rigidbody_b = readRigidIndex();
				j.position = new float[3];
				j.rotation = new float[3];
				j.const_position_1 = new float[3];
				j.const_position_2 = new float[3];
				j.const_rotation_1 = new float[3];
				j.const_rotation_2 = new float[3];
				j.spring_position = new float[3];
				j.spring_rotation = new float[3];

				getFloat(j.position);
				getFloat(j.rotation);
				getFloat(j.const_position_1);
				getFloat(j.const_position_2);
				getFloat(j.const_rotation_1);
				getFloat(j.const_rotation_2);
				getFloat(j.spring_position);
				getFloat(j.spring_rotation);
				mJoint.add(j);
			}
		}
	}
	
	private void parsePMDRigidBody() {
		int num = getInt();
		Log.d("PMDParser", "RigidBody: " + String.valueOf(num));
		if (num > 0) {
			mRigidBody = new ArrayList<RigidBody>(num);
			for (int i = 0; i < num; i++) {
				RigidBody rb = new RigidBody();

				rb.name = readTexBuf();
				readTexBuf();
				rb.bone_index = readBoneIndex();
				rb.group_index = getByte();
				rb.group_target = getShort();
				rb.shape = getByte();
				rb.size = new float[3]; // w, h, d
				rb.location = new float[3]; // x, y, z
				rb.rotation = new float[3];
				getFloat(rb.size);
				getFloat(rb.location);
				getFloat(rb.rotation);
				rb.weight = getFloat();
				rb.v_dim = getFloat();
				rb.r_dim = getFloat();
				rb.recoil = getFloat();
				rb.friction = getFloat();
				rb.type = getByte();

				rb.btrb = -1; // physics is not initialized yet
				
				if (rb.bone_index >= 0) {
					Bone b = mBone.get(rb.bone_index);
					rb.location[0] -= b.head_pos[0];
					rb.location[1] -= b.head_pos[1];
					rb.location[2] -= b.head_pos[2];
				}
				mRigidBody.add(rb);
			}
		}
	}


	private void parsePMDToonFileName(String path, String base) throws IOException {
		mToonFileName = new ArrayList<String>(11);
		mToonFileName.add(0, base + "Data/toon0.bmp");
		for (int i = 0; i < 10; i++) {
			String str = getString(100);
			str = str.replace('\\', '/');
			if (isExist(path + str)) {
				mToonFileName.add(i + 1, path + str);
			} else {
				String toon = base + "Data/" + str;
				if (!isExist(toon)) {
					mToonFileName.add(i + 1, String.format(base + "Data/toon%02d.bmp", i + 1));
					Log.d("PMDParser", String.format("Toon texture not found: %s, fall thru to default texture.", str));
				}
				mToonFileName.add(i + 1, base + "Data/" + str);
			}
		}
	}


	private void parsePMDBoneDisp() {
		int mBoneDispNum = getInt();
		Log.d("PMDParser", "BoneDisp: " + String.valueOf(mBoneDispNum));
		if (mBoneDispNum > 0) {
			mBoneDisp = new ArrayList<BoneDisp>(mBoneDispNum);
			if (mBoneDisp == null) {
				mIsPmd = false;
				return;
			}
			for (int i = 0; i < mBoneDispNum; i++) {
				BoneDisp bd = new BoneDisp();

				bd.bone_index = getShort();
				bd.bone_disp_frame_index = getByte();

				mBoneDisp.add(i, bd);
			}
		} else {
			mBoneDisp = null;
		}
	}

	private void parsePMDBoneDispName() {
		byte num = getByte();
		Log.d("PMDParser", "BoneDispName: " + String.valueOf(num));
		if (num > 0) {
			mBoneDispName = new ArrayList<String>(num);
			if (mBoneDispName == null) {
				mIsPmd = false;
				return;
			}
			for (int i = 0; i < num; i++) {
				String str = getString(50);
				mBoneDispName.add(i, str);
			}
		} else {
			mBoneDispName = null;
		}
	}

	private void parsePMDSkinDisp() {
		int num = getInt();
		Log.d("PMDParser", "SkinDisp: " + String.valueOf(num));
		if (num > 0) {
			mSkinDisp = new ArrayList<Short>(num);
			if (mSkinDisp == null) {
				mIsPmd = false;
				return;
			}
			for (int i = 0; i < num; i++) {
				Log.d("PMDParser", "SkinDisp: " + readTexBuf());
				readTexBuf();
				getByte();
				int count = getInt();
				for (int j = 0 ; j < count; j++) {
					if (getByte() == 0) {
						mSkinDisp.add(i, readBoneIndex());
					} else {
						short idx;
						if (options[ATTR_MORPH_INDEX_SZ] == 1) {
							idx = getByte();
						} else {
							idx = getShort();
						}
						mSkinDisp.add(i, idx);
					}
				}
			}
		} else {
			mSkinDisp = null;
		}
	}

	private void parsePMDFaceList() {
		int num = getInt();
		int acc = 0;
		boolean isArm = CoreLogic.isArm();
		Log.d("PMDParser", "Face: " + String.valueOf(num));
		HashMap<Integer, Integer> vertset = new HashMap<Integer, Integer>();
		if (num > 0) {
			mFace = new ArrayList<Face>(num);
			float[] buf = new float[3];
			for (int i = 0; i < num; i++) {
				Face face = new Face();

				face.name = readTexBuf();
				readTexBuf();// eng
				face.face_type = getByte(); // control panel
				int morphType = getByte();
				face.face_vert_count = getInt();

				//				face.face_vert_data = new ArrayList<FaceVertData>(face.face_vert_count);
				acc += face.face_vert_count;
				if (isArm) { // for ARM native code
					face.face_vert_index_native = ByteBuffer.allocateDirect(face.face_vert_count * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
					face.face_vert_offset_native = ByteBuffer.allocateDirect(face.face_vert_count * 4 * 3).order(ByteOrder.nativeOrder()).asFloatBuffer();
					if (morphType != 8) {
						for (int j = 0; j < face.face_vert_count; j++) {
							int v = readVertIndex();
							getFloat(buf);
							if (morphType == 1) {
								//buf[0] += mVertBuffer.get(v * 8);
								//buf[1] += mVertBuffer.get(v * 8 + 1);
								//buf[2] += mVertBuffer.get(v * 8 + 2);
								if (vertset.containsKey(v)) {
									v = vertset.get(v);
								} else {
									vertset.put(v, vertset.size());
									v = vertset.size() - 1;
								}
							}
							face.face_vert_index_native.put(v);
							face.face_vert_offset_native.put(buf);
							if (morphType == 0) {
								getFloat();
							}
							if (morphType == 2) {
								getFloat(buf); // skip rotation
								getFloat();
							}
							if (morphType >= 3 && morphType <= 7) {
								// UV
								getFloat(); // skip w
							}
						}
					} else {
						// unimplemented
						for (int j = 0; j < face.face_vert_count; j++) {
							if (options[ATTR_MATERIAL_INDEX_SZ] == 1) {
								getByte();
							} else {
								getShort();
							}
							face.face_vert_index_native.put(0);
							getByte();
							getFloat(buf);
							face.face_vert_offset_native.put(buf);
							getFloat(buf);
							getFloat(buf);
							getFloat(buf);
							getFloat(buf);
							getFloat(buf);
							getFloat(buf);
							getFloat(buf);
							getFloat(buf);
							getFloat();
						}
						face.face_vert_count = 0;
					}
					face.face_vert_index_native.position(0);
					face.face_vert_offset_native.position(0);
				} else { // for universal code
					face.face_vert_base = new float[face.face_vert_count * 3];
					face.face_vert_cleared = new boolean[face.face_vert_count];
					face.face_vert_updated = new boolean[face.face_vert_count];
					face.face_vert_index = new int[face.face_vert_count];
					face.face_vert_offset = new float[face.face_vert_count * 3];

					for (int j = 0; j < face.face_vert_count; j++) {
						face.face_vert_index[j] = face.face_type == 0 ? mInvMap[getInt()] : getInt();
						face.face_vert_offset[j * 3 + 0] = getFloat();
						face.face_vert_offset[j * 3 + 1] = getFloat();
						face.face_vert_offset[j * 3 + 2] = getFloat();
						face.face_vert_cleared[j] = true;
					}
				}
				
				mFace.add(i, face);
			}
			
			Face face = new Face();
			face.name = "base";
			face.face_type = 0;
			face.face_vert_count = vertset.size();
			if (isArm) { // for ARM native code
				face.face_vert_index_native = ByteBuffer.allocateDirect(face.face_vert_count * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
				face.face_vert_offset_native = ByteBuffer.allocateDirect(face.face_vert_count * 4 * 3).order(ByteOrder.nativeOrder()).asFloatBuffer();
				for (Entry<Integer, Integer> v : vertset.entrySet()) {
					int idx = v.getKey();
					face.face_vert_index_native.position(v.getValue());
					face.face_vert_offset_native.position(v.getValue() * 3);
					face.face_vert_index_native.put(idx);
					
					buf[0] = mVertBuffer.get(idx * 8);
					buf[1] = mVertBuffer.get(idx * 8 + 1);
					buf[2] = mVertBuffer.get(idx * 8 + 2);
					face.face_vert_offset_native.put(buf);
				}
				face.face_vert_index_native.position(0);
				face.face_vert_offset_native.position(0);
			}
			mFace.add(face);
			Log.d("PMDParser", String.format("Total Face Vert: %d", acc));
		} else {
			mFace = null;
		}
		mInvMap = null;
	}


	private void parsePMDBoneList() {
		// the number of Vertexes
		int num = getInt();
		mIK = new ArrayList<IK>();
		Log.d("PMDParser", "BONE: " + String.valueOf(num));
		if (num > 0) {
			mBone = new ArrayList<Bone>(num);
			for (int i = 0; i < num; i++) {
				Bone bone = new Bone();

				bone.name = readTexBuf();
				try {
					bone.name_bytes = bone.name.getBytes("UTF-16LE");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				} // getStringBytes(new byte[20], 20);
				readTexBuf(); // English name

				bone.head_pos = new float[4];
				bone.head_pos[0] = getFloat();
				bone.head_pos[1] = getFloat();
				bone.head_pos[2] = getFloat();
				bone.head_pos[3] = 1; // for IK (Miku:getCurrentPosition:Matrix.multiplyMV(v, 0, d, 0, b.head_pos, 0)

				bone.parent = readBoneIndex();
				getInt();

				short flags = getShort();
				
				if ((flags & 1) == 0) {
					getFloat();
					getFloat();
					getFloat();
				} else {
					readBoneIndex();
				}
				
				if ((flags & 0x0100) != 0) {
					readBoneIndex();
					getFloat();
				}
				
				if ((flags & 0x0400) != 0) {
					bone.is_leg = true;
					getFloat();
					getFloat();
					getFloat();
				}
				if ((flags & 0x0800) != 0) {
					getFloat();
					getFloat();
					getFloat();
					getFloat();
					getFloat();
					getFloat();
				}
				if ((flags & 0x2000) != 0) {
					getInt();
				}
				if ((flags & 0x0020) != 0) {
					bone.ik = (short)mIK.size();
					
					IK ik = new IK();
					ik.ik_bone_index = i;
					ik.ik_target_bone_index = readBoneIndex();
					ik.ik_chain_length = 0;
					ik.iterations = getInt();
					ik.control_weight = getFloat();
					
					int iklinks = getInt();
					ik.ik_chain_length = (byte)iklinks;
					ik.ik_child_bone_index = new Short[iklinks];
					
					for (int ikc = 0; ikc < iklinks; ikc ++) {
						ik.ik_child_bone_index[ikc] = readBoneIndex();
						if (getByte() == 1) {
							getFloat();
							getFloat();
							getFloat();
							getFloat();
							getFloat();
							getFloat();
						}
					}
					mIK.add(ik);
				}
				//bone.tail = getShort();
				//bone.type = getByte();


				bone.motion = null;
				bone.quaternion = new double[4]; // for skin-mesh preCalkIK
				bone.matrix = new float[16]; // for skin-mesh animation
				bone.matrix_current = new float[16]; // for temporary (current bone matrix that is not include parent rotation
				bone.updated = false; // whether matrix is updated by VMD or not
				bone.is_leg |= bone.name.contains("\u3072\u3056"); // HI,ZA in HIRAGANA.  ("ひざ"　文字化け回避用にエスケープ)

				if (bone.tail != -1) {
					mBone.add(i, bone);
				}
			}
		} else {
			mBone = null;
		}
	}

	private void parsePMDMaterialList(String path) {
		int texNum = getInt();
		Log.d("PMDParser", "TEXTURE: " + String.valueOf(texNum));
		String[] texList = new String[texNum];
		for (int i = 0; i< texNum; i++) {
			texList[i] = readTexBuf();
		}

		// the number of Vertexes
		int num = getInt();
		Log.d("PMDParser", "MATERIAL: " + String.valueOf(num));
		if (num > 0) {
			mMaterial = new ArrayList<Material>(num);
			int acc = 0;
			for (int i = 0; i < num; i++) {
				Material material = new Material();
				Log.d("PMDParser", "MATERIAL: name:" + readTexBuf());
				readTexBuf(); // engName

				material.diffuse_color = new float[4];
				material.specular_color = new float[3];
				material.emmisive_color = new float[3];

				getFloat(material.diffuse_color);
				getFloat(material.specular_color);
				material.power = getFloat();
				getFloat(material.emmisive_color);

				material.edge_flag = getByte();
				
				// edge color, size
				getFloat();
				getFloat();
				getFloat();
				getFloat();
				getFloat();
				
				int tn = getByte();
				if (tn >= 0) {
					material.texture = texList[tn];
				}
				int sn = getByte();
				if (sn >= 0) {
					material.sphere = texList[sn];
				}
				
				byte sphereMode = getByte();
				if (sphereMode == 0) {
					material.sphere = null;
				}
				byte toonMode = getByte();

				material.toon_index = getByte();
				material.toon_index += 1; // 0xFF to toon0.bmp, 0x00 to toon01.bmp, 0x01 to toon02.bmp...
				if (toonMode == 0) {
					material.toon_index = 0;
				}
				readTexBuf(); // memo
				material.face_vert_count = getInt();
				if (material.texture != null) {
					material.texture = path + material.texture;
					if (!new File(material.texture).exists()) {
						mIsPmd = false;
						Log.d("PMDParser", String.format("Texture not found: %s", material.texture));
					}
				}
				if (material.sphere != null) {
					material.sphere = path + material.sphere;
					if (!new File(material.sphere).exists()) {
						material.sphere = null; // fake
						//						mIsPmd = false;
						//						Log.d("PMDParser", String.format("Sphere map texture not found: %s", material.sphere));
					}
				}

				material.face_vert_offset = acc;

				acc = acc + material.face_vert_count;
				mMaterial.add(i, material);
			}
			Log.d("PMDParser", "CHECKSUM IN MATERIAL: " + String.valueOf(acc));
		} else {
			mMaterial = null;
		}

	}

	private void parsePMDIndexList() {
		// the number of Vertexes
		int num = getInt();
		Log.d("PMDParser", "INDEX: " + String.valueOf(num));
		if (num > 0) {
			mInvMap = new int[mVertBuffer.capacity() / 8];
			for (int i = 0; i < mInvMap.length; i++) {
				mInvMap[i] = i; // dummy
			}
			mIndexBuffer = ByteBuffer.allocateDirect(num * 2).order(ByteOrder.nativeOrder()).asShortBuffer();
			for (int i = 0; i < num; i++) {
				int vi = readVertIndex();
				mIndexBuffer.put((short) vi);
			}
			mIndexBuffer.position(0);
			mVertBuffer.position(0);
			mWeightBuffer.position(0);
		} else {
			mIndexBuffer = null;
			mVertBuffer = null;
			mWeightBuffer = null;
		}
	}
	
	private short readBoneIndex() {
		if (options[ATTR_BONE_INDEX_SZ] == 1) {
			return getByte();
		} else if (options[ATTR_BONE_INDEX_SZ] == 2) {
				return getShort();
		} else {
			return (short)getInt(); // !!! max bones = 0xffff
		}
	}

	private short readRigidIndex() {
		if (options[ATTR_RIGIDBODY_INDEX_SZ] == 1) {
			return getByte();
		} else if (options[ATTR_RIGIDBODY_INDEX_SZ] == 2) {
				return getShort();
		} else {
			return (short)getInt(); // !!! max bones = 0xffff
		}
	}

	private int readVertIndex() {
		if (options[ATTR_VERT_INDEX_SZ] == 1) {
			return getByte() & 0xff;
		} else if (options[ATTR_VERT_INDEX_SZ] == 2) {
				return getShort() &0xffff;
		} else {
			return getInt();
		}
	}

	private void parsePMDVertexList() {
		// the number of Vertexes
		float[] vbuf = new float[8];
		int num = getInt();
		Log.d("PMDParser", "VERTEX: " + String.valueOf(num) + " opt uv:" + options[ATTR_OPTION_UV]);
		if (num > 0) {
			mVertexPos = position();
			mVertBuffer = ByteBuffer.allocateDirect(num * 4 * 8).order(ByteOrder.nativeOrder()).asFloatBuffer();
			mWeightBuffer = ByteBuffer.allocate(num * 2 * 3).asShortBuffer();
			
			for (int i = 0 ; i< num; i++) {
				getFloat(vbuf);
				mVertBuffer.put(vbuf);
				for (int j = 0; j < options[ATTR_OPTION_UV]; j++) {
					getFloat();
					getFloat();
					getFloat();
					getFloat();
				}
				byte deftype = getByte();
				
				short bone_num_0;
				short bone_num_1;
				float bone_weight;
				if (deftype == 0) {
					// 0:BDEF1
					bone_num_0 = readBoneIndex();
					bone_num_1 = 0;
					bone_weight = 1.0f;
				} else if (deftype == 1) {
					// 1:BDEF2
					bone_num_0 = readBoneIndex();
					bone_num_1 = readBoneIndex();
					bone_weight = getFloat();
				} else if (deftype == 2) {
					// 2:BDEF4 (unimplemented)
					bone_num_0 = readBoneIndex();
					bone_num_1 = readBoneIndex();
					readBoneIndex();
					readBoneIndex();
					bone_weight = getFloat();
					getFloat();
					getFloat();
					getFloat();
				} else if (deftype == 3) {
					// 3:SDEF (unimplemented)
					bone_num_0 = readBoneIndex();
					bone_num_1 = readBoneIndex();
					bone_weight = getFloat();
					for (int j = 0; j < 9; j++) {
						getFloat();
					}
				} else {
					bone_num_0 = 0;
					bone_num_1 = 0;
					bone_weight = 1.0f;
				}
				if (bone_weight != 1.0f) {
					mIsOneSkinning = false;
				}
				if (bone_weight < 0.5f) {
					bone_weight = 1.0f - bone_weight;
					short t = bone_num_0;
					bone_num_0 = bone_num_1;
					bone_num_1 = t;
				}
				mWeightBuffer.put(bone_num_0);
				mWeightBuffer.put(bone_num_1);
				mWeightBuffer.put((short)(bone_weight * 100));
				
				getFloat(); // edgeScale
			}
			
			// position(mVertexPos + num * 38);
		} else {
			mVertBuffer = null;
		}
	}

	private byte[] parsePMXHeader() {
		// Magic
		String s = getString(4);
		Log.d("PMDParser", "MAGIC: " + s);
		if (!s.equals("PMX ")) {
			return null;
		}

		// Version
		float f = getFloat();
		Log.d("PMDParser", "VERSION: " + String.valueOf(f));

		// option
		int sz = getByte();
		byte[] options = new byte[sz];
		getBytes(options, sz);

		// Model Name
		mModelName = readTexBuf(options[ATTR_STRING_ENCODING]);
		Log.d("PMDParser", "MODEL NAME: " + mModelName);

		mEnglishModelName = readTexBuf(options[ATTR_STRING_ENCODING]);

		// description
		mDescription = readTexBuf(options[ATTR_STRING_ENCODING]);
		Log.d("PMDParser", "DESCRIPTION: " + mDescription);

		mEnglishComment = readTexBuf(options[ATTR_STRING_ENCODING]);

		mIsPmd = true;
		return options;
	}

	private String readTexBuf() {
		return readTexBuf(options[ATTR_STRING_ENCODING]);
	}
	private String readTexBuf(byte encode) {
		int len = getInt();
		if (len == 0) {
			return "";
		}
		byte[] buf = new byte[len];
		getBytes(buf, len);
		try {
			if (encode == 0) {
				return new String(buf, "UTF-16LE");
			} else if (encode == 1) {
				return new String(buf, "UTF-8");
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}

	public boolean isPmd() {
		return mIsPmd;
	}

	public FloatBuffer getVertexBuffer() {
		return mVertBuffer;
	}

	public IntBuffer getIndexBufferI() {
		return null;
	}

	public ShortBuffer getIndexBufferS() {
		return mIndexBuffer;
	}

	public ShortBuffer getWeightBuffer() {
		return mWeightBuffer;
	}

	public ArrayList<Vertex> getVertex() {
		return null;
	}

	public ArrayList<Integer> getIndex() {
		return null;
	}

	public ArrayList<Material> getMaterial() {
		return mMaterial;
	}

	public ArrayList<Bone> getBone() {
		return mBone;
	}

	public ArrayList<String> getToonFileName() {
		return mToonFileName;
	}

	public ArrayList<IK> getIK() {
		return mIK;
	}

	public ArrayList<Face> getFace() {
		return mFace;
	}

	public ArrayList<RigidBody> getRigidBody() {
		return mRigidBody;
	}

	public ArrayList<Joint> getJoint() {
		return mJoint;
	}

	public String getFileName() {
		return mFileName;
	}

	public boolean isOneSkinning() {
		return mIsOneSkinning;
	}

	public void recycle() {
		mModelName = null;
		mDescription = null;
		mVertBuffer = null;
		mIndexBuffer = null;
		mWeightBuffer = null;

		mSkinDisp = null;
		mEnglishModelName = null;
		mEnglishComment = null;
		mEnglishBoneName = null;
		mEnglishSkinName = null;
		mEnglishBoneDispName = null;
		close();
	}

	public void recycleVertex() {
		mVertBuffer = null;
	}
}
