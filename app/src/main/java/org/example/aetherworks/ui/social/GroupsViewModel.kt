package org.example.aetherworks.ui.social

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.example.aetherworks.storage.db.AetherDatabase
import org.example.aetherworks.storage.db.dao.GroupWithMembers
import org.example.aetherworks.storage.db.entity.GroupMember
import org.example.aetherworks.storage.db.entity.TrustGroup

class GroupsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AetherDatabase.getPrivateDatabase()
    private val groupDao = db.groupDao()

    private val _groups = MutableStateFlow<List<GroupWithMembers>>(emptyList())
    val groups: StateFlow<List<GroupWithMembers>> = _groups

    init {
        viewModelScope.launch {
            groupDao.getAllGroups().collect { trustGroups ->
                val groupsWithMembers = trustGroups.map { group ->
                    val members = groupDao.getGroupMembersSync(group.groupId)
                    GroupWithMembers(group, members)
                }
                _groups.value = groupsWithMembers
            }
        }
    }

    fun createGroup(name: String) {
        viewModelScope.launch {
            groupDao.insertGroup(TrustGroup(name = name))
        }
    }

    fun addMemberToGroup(groupId: String, publicKeyBase64: String) {
        viewModelScope.launch {
            groupDao.insertGroupMember(GroupMember(groupId, publicKeyBase64))
        }
    }

    fun removeMemberFromGroup(groupId: String, publicKeyBase64: String) {
        viewModelScope.launch {
            groupDao.removeMember(groupId, publicKeyBase64)
        }
    }
}
