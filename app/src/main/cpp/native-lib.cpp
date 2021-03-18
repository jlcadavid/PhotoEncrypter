#include <jni.h>
#include <string>
#include "AES/aes.c"
#include "AES/aes.h"

//CRYPT CONFIG
#define MAX_LEN (4*1024*1024)
#define ENCRYPT 0
#define DECRYPT 1
#define AES_KEY_SIZE 256
#define READ_LEN 10

#define TARGET_CLASS "com/example/mohsinmustafa1/aesndk/Helper/AES"
#define TARGET_CRYPT "crypt"
#define TARGET_CRYPT_SIG "([BJI)[B"
#define TARGET_READ "read"
#define TARGET_READ_SIG "(Ljava/lang/String;J)[B"

//AES_IV
static unsigned char AES_IV[16] = { 0x74 ,0x68 ,0x69 ,0x73 ,0x20 ,0x69 ,0x74 ,0x20 ,0x74 ,0x68 ,0x65 ,0x20 ,0x6b ,0x65 ,0x79 ,0x2e };
//AES_KEY
/*static unsigned char AES_KEY[32] = { 0x62 ,0x72 ,0x65 ,0x61 ,0x6b ,0x6d ,0x65 ,0x69 ,0x66 ,
                                     0x75 ,0x63 ,0x61 ,0x6e ,0x62 ,0x62 ,0x79 ,0x62 ,0x72 ,
                                     0x65 ,0x61 ,0x6b ,0x6d ,0x65 ,0x69 ,0x66 ,0x75 ,0x63 ,
                                     0x61 ,0x6e ,0x62 ,0x62 ,0x79 };*/
//Key = this it the key.
static unsigned char AES_KEY[16] = { 0x74 ,0x68 ,0x69 ,0x73 ,0x20 ,0x69 ,0x74 ,0x20 ,0x74 ,0x68 ,0x65 ,0x20 ,0x6b ,0x65 ,0x79 ,0x2e };

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_example_photoencrypter_AES_encrypt_1byte_1array(JNIEnv *env, jclass type, jbyteArray data_,
                                                         jbyteArray key_) {
    //CHECK INPUT DATA
    unsigned int len = (unsigned int) (env -> GetArrayLength(data_));
    if (len <= 0 || len >= MAX_LEN) {
        return  NULL;
    }

    unsigned char *byteArrayData = (unsigned char*) env -> GetByteArrayElements(data_, NULL);
    unsigned char *encryptionKey = (unsigned char*) env -> GetByteArrayElements(key_, NULL);

    if (!byteArrayData) {
        return NULL;
    }

    unsigned int rest_len = len % AES_BLOCK_SIZE;
    unsigned int padding_len = AES_BLOCK_SIZE - rest_len;
    unsigned src_len = len + padding_len;

    unsigned char *input = (unsigned char*) malloc(src_len);
    memset(input, 0, src_len);
    memcpy(input, byteArrayData, len);

    if (padding_len > 0) {
        memset(input + len, (unsigned char) padding_len, padding_len);
    }

    unsigned char* buff = (unsigned char*) malloc(src_len);
    if (!buff) {
        free(input);
        return NULL;
    }
    memset(buff, 0, src_len);

    //SET KEY & IV
    unsigned int key_schedule[AES_BLOCK_SIZE * 4] = { 0 };
    aes_key_setup(encryptionKey, key_schedule, AES_KEY_SIZE);

    aes_encrypt_cbc(input, src_len, buff, key_schedule, AES_KEY_SIZE, AES_IV);

    jbyteArray bytes = env -> NewByteArray(src_len);
    env -> SetByteArrayRegion(bytes, 0, src_len, (jbyte*) buff);

    free(input);
    free(buff);

    env -> ReleaseByteArrayElements(data_, reinterpret_cast<jbyte *>(byteArrayData), 0);
    env -> ReleaseByteArrayElements(key_, reinterpret_cast<jbyte *>(encryptionKey), 0);

    return bytes;
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_example_photoencrypter_AES_decrypt_1byte_1array(JNIEnv *env, jclass type, jbyteArray data_,
                                                         jbyteArray key_) {
    // TODO
    //CHECK INPUT DATA
    unsigned int len = (unsigned int) (env -> GetArrayLength(data_));
    if (len <= 0 || len >= MAX_LEN) {
        return  NULL;
    }

    unsigned char *byteArrayData = (unsigned char*) env -> GetByteArrayElements(data_, NULL);
    unsigned char *encryptionKey = (unsigned char*) env -> GetByteArrayElements(key_, NULL);

    if (!byteArrayData) {
        return NULL;
    }

    unsigned int rest_len = len % AES_BLOCK_SIZE;
    unsigned int padding_len = 0;
    unsigned src_len = len + padding_len;

    unsigned char *input = (unsigned char*) malloc(src_len);
    memset(input, 0, src_len);
    memcpy(input, byteArrayData, len);

    if (padding_len > 0) {
        memset(input + len, (unsigned char) padding_len, padding_len);
    }

    env->ReleaseByteArrayElements(data_, reinterpret_cast<jbyte *>(byteArrayData), 0);

    unsigned char* buff = (unsigned char*) malloc(src_len);
    if (!buff) {
        free(input);
        return NULL;
    }
    memset(buff, 0, src_len);

    //SET KEY & IV
    unsigned int key_schedule[AES_BLOCK_SIZE * 4] = { 0 };
    aes_key_setup(encryptionKey, key_schedule, AES_KEY_SIZE);

    aes_decrypt_cbc(input, src_len, buff, key_schedule, AES_KEY_SIZE, AES_IV);

    unsigned char* ptr = buff;
    ptr += (src_len - 1);
    padding_len = (unsigned int) *ptr;
    if (padding_len > 0 && padding_len <= AES_BLOCK_SIZE) {
        src_len -= padding_len;
    }
    ptr = NULL;

    jbyteArray bytes = env -> NewByteArray(src_len);
    env -> SetByteArrayRegion(bytes, 0, src_len, (jbyte*) buff);

    free(input);
    free(buff);

    env -> ReleaseByteArrayElements(data_, reinterpret_cast<jbyte *>(byteArrayData), 0);
    env -> ReleaseByteArrayElements(key_, reinterpret_cast<jbyte *>(encryptionKey), 0);

    return bytes;
}