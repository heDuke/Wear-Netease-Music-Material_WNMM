package com.refresh.netmusic.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.refresh.netmusic.data.SessionStore
import com.refresh.netmusic.ncm.api.NeteaseApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val qrurl: String = "",
    val statusText: String = "正在生成二维码",
    val loggedIn: Boolean = false,
    val error: String? = null,
)

class LoginViewModel(
    private val api: NeteaseApi,
    private val session: SessionStore,
) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            if (api.request.cookies.hasLogin()) {
                runCatching { api.loginStatus() }.onSuccess { profile ->
                    if (profile != null) {
                        _state.value = LoginUiState(loggedIn = true, statusText = profile.nickname)
                        return@launch
                    }
                }
            }
            refreshQr()
        }
    }

    fun refreshQr() {
        viewModelScope.launch {
            runCatching {
                val key = api.loginQrKey().unikey
                val url = api.loginQrCreate(key).qrurl
                _state.value = LoginUiState(qrurl = url, statusText = "用网易云扫描")
                while (true) {
                    delay(2000)
                    val check = api.loginQrCheck(key)
                    when (check.code) {
                        801 -> _state.value = _state.value.copy(statusText = "用网易云扫描")
                        802 -> _state.value = _state.value.copy(statusText = "已扫码，请确认")
                        803 -> {
                            session.saveCookies(api.request.cookies.asHeader())
                            _state.value = _state.value.copy(loggedIn = true, statusText = "登录成功")
                            break
                        }
                        800 -> {
                            _state.value = _state.value.copy(statusText = "已过期，点刷新")
                            break
                        }
                    }
                }
            }.onFailure {
                _state.value = _state.value.copy(error = it.message, statusText = "生成失败")
            }
        }
    }
}
