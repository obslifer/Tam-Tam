package com.example.tam_tam.models

interface RealmCallback<T> {
    fun onSuccess(result: T)
    fun onError(error: Throwable)
}
