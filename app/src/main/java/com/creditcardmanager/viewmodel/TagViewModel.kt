package com.creditcardmanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.creditcardmanager.data.repository.TagRepository
import com.creditcardmanager.model.Tag
import com.creditcardmanager.utils.IdGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TagViewModel @Inject constructor(private val tagRepo: TagRepository) : ViewModel() {
    private val _tags = MutableStateFlow<List<Tag>>(emptyList())
    val tags: StateFlow<List<Tag>> = _tags.asStateFlow()
    init { viewModelScope.launch { tagRepo.getAllTags().collect { _tags.value = it } } }
    fun addTag(name: String, color: String? = null) { viewModelScope.launch { tagRepo.saveTag(Tag(id = IdGenerator.generateTagId(), name = name, color = color)) } }
    fun updateTag(tag: Tag) { viewModelScope.launch { tagRepo.updateTag(tag) } }
    fun deleteTag(tag: Tag) { viewModelScope.launch { tagRepo.deleteTag(tag) } }
}
