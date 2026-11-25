//! Ponte JNI entre o Kotlin (app.kleinmusic.adblock.NativeAdBlock) e o motor adblock-rust do Brave.
//! Nesta etapa: só bloqueio de requisições de rede.

use adblock::lists::ParseOptions;
use adblock::request::Request;
use adblock::Engine;
use jni::objects::{JClass, JString};
use jni::sys::{jboolean, jlong, JNI_FALSE, JNI_TRUE};
use jni::JNIEnv;

fn read(env: &mut JNIEnv, s: &JString) -> Option<String> {
    env.get_string(s).ok().map(|v| v.into())
}

/// Cria o motor a partir do texto de uma lista de filtros (uma regra por linha).
/// Devolve um "endereço" que o Kotlin guarda e passa de volta nas outras chamadas. 0 = erro.
#[no_mangle]
pub extern "system" fn Java_app_kleinmusic_adblock_NativeAdBlock_nativeCreate(
    mut env: JNIEnv,
    _class: JClass,
    rules: JString,
) -> jlong {
    let Some(text) = read(&mut env, &rules) else { return 0 };
    let engine = Engine::from_rules(text.lines(), ParseOptions::default());
    Box::into_raw(Box::new(engine)) as jlong
}

/// Responde se a requisição deve ser bloqueada.
/// request_type segue os nomes do adblock-rust: "script", "image", "stylesheet", "xmlhttprequest",
/// "sub_frame", "main_frame", "other" etc.
#[no_mangle]
pub extern "system" fn Java_app_kleinmusic_adblock_NativeAdBlock_nativeShouldBlock(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    url: JString,
    source_url: JString,
    request_type: JString,
) -> jboolean {
    if handle == 0 {
        return JNI_FALSE;
    }
    let (Some(url), Some(source), Some(kind)) = (
        read(&mut env, &url),
        read(&mut env, &source_url),
        read(&mut env, &request_type),
    ) else {
        return JNI_FALSE;
    };
    let engine = unsafe { &*(handle as *const Engine) };
    match Request::new(&url, &source, &kind) {
        Ok(request) if engine.check_network_request(&request).matched => JNI_TRUE,
        _ => JNI_FALSE,
    }
}

/// Libera a memória do motor.
#[no_mangle]
pub extern "system" fn Java_app_kleinmusic_adblock_NativeAdBlock_nativeDestroy(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) {
    if handle != 0 {
        unsafe { drop(Box::from_raw(handle as *mut Engine)) };
    }
}
