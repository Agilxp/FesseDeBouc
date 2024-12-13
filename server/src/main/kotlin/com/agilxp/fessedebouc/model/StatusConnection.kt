package com.agilxp.fessedebouc.model

class StatusConnection(val userStatus: UserStatusDTO) {
    fun hasChanges(newUserStatus: UserStatusDTO): Boolean {
        return userStatus != newUserStatus
    }
}