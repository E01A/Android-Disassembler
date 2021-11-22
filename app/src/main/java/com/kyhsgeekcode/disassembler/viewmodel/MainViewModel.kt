package com.kyhsgeekcode.disassembler.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kyhsgeekcode.TAG
import com.kyhsgeekcode.disassembler.*
import com.kyhsgeekcode.disassembler.project.ProjectManager
import com.kyhsgeekcode.disassembler.project.models.ProjectModel
import com.kyhsgeekcode.disassembler.project.models.ProjectType
import com.kyhsgeekcode.filechooser.model.FileItem
import com.kyhsgeekcode.filechooser.model.FileItemApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {
    sealed class Event {
        object NavigateToSettings : Event()
        data class StartProgress(val dummy: Unit = Unit) : Event()
        data class FinishProgress(val dummy: Unit = Unit) : Event()
        data class AlertError(val text: String) : Event()

        data class ShowSnackBar(val text: String) : Event()

        data class ShowToast(val text: String) : Event()
    }

    private val eventChannel = Channel<Event>(Channel.BUFFERED)
    val eventsFlow = eventChannel.receiveAsFlow()

    private val _askCopy = MutableStateFlow(false)
    val askCopy = _askCopy as StateFlow<Boolean>

    private val _askOpen = MutableStateFlow<Pair<Int, FileDrawerListItem>?>(null)
    val askOpen = _askOpen as StateFlow<Pair<Int, FileDrawerListItem>?>

    private val _file = MutableStateFlow<File>(File("/"))
    val file = _file as StateFlow<File>

    private val _nativeFile = MutableStateFlow<File?>(null)
    val nativeFile = _nativeFile as StateFlow<File?>

    private val _projectType = MutableStateFlow<String>(ProjectType.UNKNOWN)
    val projectType = _projectType as StateFlow<String>

    private val _openAsProject = MutableStateFlow(false)
    val openAsProject = _openAsProject as StateFlow<Boolean>

    private val _selectedFilePath = MutableStateFlow("")
    val selectedFilePath = _selectedFilePath as StateFlow<String>

    private val _currentProject = MutableStateFlow<ProjectModel?>(null)
    val currentProject = _currentProject as StateFlow<ProjectModel?>

    val fileDrawerListViewModel =
        object : ExpandableListViewModel<FileDrawerListItem>() {
            override fun onClickItem(item: FileDrawerListItem) {
                TODO("Not yet implemented")
            }
        }

//        //{
//
//            FileDrawerListItem(
//                pm.rootFile,
//                0
//            )
//        }

    init {
        viewModelScope.launch {
            currentProject.filterNotNull().collect { pm ->
//                fileDrawerListViewModel._items.value = listOf()
            }
        }
    }

    fun onSelectIntent(intent: Intent) {
        Timber.d("onActivityResultOk")
        _openAsProject.value = intent.getBooleanExtra("openProject", false)
        val fi = intent.getSerializableExtra("fileItem") as? FileItem
        if (fi != null) {
            onSelectFileItem(fi)
        } else {
            val uri = intent.getParcelableExtra("uri") as Uri?
                ?: intent.getBundleExtra("extras")?.get(Intent.EXTRA_STREAM) as Uri?
                ?: return
            onSelectUri(uri)
        }
    }

    private fun onSelectFileItem(fileItem: FileItem) {
        _file.value = fileItem.file ?: run {
            Logger.e(TAG, "Failed to load fileItem: $fileItem")
            return@onSelectFileItem
        }
        _nativeFile.value = if (fileItem is FileItemApp) {
            fileItem.nativeFile
        } else {
            null
        }
        _projectType.value = fileItemTypeToProjectType(fileItem)
        _askCopy.value = true
    }

    private fun onSelectUri(uri: Uri) {
        if (uri.scheme == "content") {
            try {
                val app = getApplication<Application>()
                app.contentResolver.openInputStream(uri).use { inStream ->
                    val file = app.getExternalFilesDir(null)?.resolve("tmp")?.resolve("openDirect")
                        ?: return
                    file.parentFile.mkdirs()
                    file.outputStream().use { fileOut ->
                        inStream?.copyTo(fileOut)
                    }
                    val project =
                        ProjectManager.newProject(file, ProjectType.UNKNOWN, file.name, true)
                    _selectedFilePath.value = project.sourceFilePath
                    _currentProject.value = project
                }
            } catch (e: Exception) {
                viewModelScope.launch {
                    eventChannel.send(Event.FinishProgress())
                    eventChannel.send(Event.AlertError("Failed to create project"))
                }
            }
        }
    }

    fun onCopy(copy: Boolean) {
        _askCopy.value = false
        CoroutineScope(Dispatchers.Main).launch {
            eventChannel.send(Event.StartProgress())
            try {
                val project = withContext(Dispatchers.IO) {
                    onClickCopyDialog(copy)
                }
                _selectedFilePath.value = project.sourceFilePath
                _currentProject.value = project
            } catch (e: Exception) {
                eventChannel.send(Event.AlertError("Failed to create project"))
            }
            eventChannel.send(Event.FinishProgress())
        }
    }

    private fun onClickCopyDialog(
        copy: Boolean
    ): ProjectModel {
        val project =
            ProjectManager.newProject(file.value, projectType.value, file.value.name, copy)
        if (copy) {
            copyNativeDirToProject(nativeFile.value, project)
        }
        return project
    }

//    fun onDrawerItemClick(index: Int, item: FileDrawerListItem) {
//        // Ask to open raw or not. not -> expand only.
//        // ask opening. ok -> open.
//        if (item.isOpenable) {
//            _askOpen.value = Pair(index, item)
//        } else if (item.isExpandable) {
//            if (isExpanded(item)) {
//                expandDrawerItem(index, item)
//            }
//        }
//    }

//    private fun expandDrawerItem(index: Int, item: FileDrawerListItem) {
//        val subItems = item.getSubObjects()
//        val newList = ArrayList(fileDrawerItems.value)
//        newList.addAll(index + 1, subItems)
//        _fileDrawerItems.value = newList
//    }
//
//    private fun collapseDrawerItem(item: Pair<Int, FileDrawerListItem>) {
//        val idx = item.first
//        val level = item.second.level
//        val items = ArrayList(fileDrawerItems.value)
//        var start = false
//        var done = false
//
//    }

//    fun onOpen(open: Boolean, item: Pair<Int, FileDrawerListItem>) {
//        _askOpen.value = null
//        if (open) {
//            openDrawerItem(item.second)
//        } else if (item.second.isExpandable) {
//            if (isExpanded(item)) {
//                collapseDrawerItem(item)
//            } else {
//                expandDrawerItem(item.first, item.second)
//            }
//        }
//    }

    private fun openDrawerItem(item: FileDrawerListItem) {

    }
//
//    fun isExpanded(item: Pair<Int, FileDrawerListItem>): Boolean {
//        val items = fileDrawerItems.value
//        val idx = item.first
//        if (items.size > idx + 1) {
//            val nextItem = items[idx + 1]
//            return nextItem.level == item.second.level + 1
//        }
//        return false
//    }

    private val _parsedFile: StateFlow<AbstractFile?> = MutableStateFlow<AbstractFile?>(null)
    val parsedFile: StateFlow<AbstractFile?> = _parsedFile
}

fun fileItemTypeToProjectType(fileItem: FileItem): String {
    if (fileItem is FileItemApp)
        return ProjectType.APK
    return ProjectType.UNKNOWN
}

fun copyNativeDirToProject(nativeFile: File?, project: ProjectModel) {
    if (nativeFile != null && nativeFile.exists() && nativeFile.canRead()) {
        val targetFolder = File(project.sourceFilePath + "_libs")
        targetFolder.mkdirs()
        var targetFile = targetFolder.resolve(nativeFile.name)
        var i = 0
        while (targetFile.exists()) {
            targetFile = File(targetFile.absolutePath + "_extracted_$i.so")
            i++
        }
        // FileUtils.copyDirectory(nativeFile, targetFile)
        copyDirectory(nativeFile, targetFile)
    }
}

