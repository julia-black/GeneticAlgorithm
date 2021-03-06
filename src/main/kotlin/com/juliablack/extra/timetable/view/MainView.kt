package com.juliablack.extra.timetable.view

import com.github.thomasnield.rxkotlinfx.actionEvents
import com.juliablack.extra.timetable.controller.EventController
import com.juliablack.extra.timetable.logic.genetic.GeneratorTimetable
import com.juliablack.extra.timetable.logic.genetic.timetable.GroupTimetableForView
import com.juliablack.extra.timetable.logic.genetic.timetable.Timetable
import com.juliablack.extra.timetable.util.Settings
import com.juliablack.extra.timetable.util.Util
import javafx.application.Platform
import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType
import javafx.scene.control.ButtonType.OK
import javafx.scene.control.TextField
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.BorderPane
import javafx.stage.FileChooser
import javafx.stage.Stage
import tornadofx.*
import java.util.*

class MainView : View() {

    override val root = BorderPane()

    private val controller: EventController by inject()

    private var imageView: ImageView? = null

    private var groupNumberField: TextField? = null

    private var borderPane: BorderPane = borderpane {
        center {
            imageView = imageview("/app/import_file.png") {
                fitWidth = 390.0
                fitHeight = 250.0
            }
        }
    }

    init {
        title = "ExtraTimeTable"
        showView()
        subscribeAllEvent()
    }

    private fun showView() {
        with(root) {
            setPrefSize(940.0, 610.0)
            top {
                this += menubar {
                    menu("Сгенерировать расписание") {
                        item("Из Excel-файла").apply {
                            actionEvents()
                                    .map { Unit }
                                    .subscribe(controller.showViewOpenFile)
                        }
                        item("Из внутренней базы данных").apply {
                            actionEvents()
                                    .map { Unit }
                                    .subscribe(controller.generateTimetable)
                        }
                    }
                    menu("Настройки") {
                        item("Изменить").apply {
                            actionEvents()
                                    .map { Unit }
                                    .subscribe(controller.openSettings)
                        }
                    }
                }
            }
            center {
                this += borderPane
            }
            bottom {
                add<ProgressView>()
            }
        }
    }

    private fun subscribeAllEvent() {
        controller.generateTimetable
                .subscribe {
                    runAsync {

                        updateTitle("Загрузка данных")

                        try {
                            val generatorTimeTable = GeneratorTimetable(Settings.optimalLessonsOfDay,
                                    Settings.maxLessonsOfDay - 1)
                            val allProgress = (Settings.countOfPopulation + Settings.countCycle) * Settings.count
                            var progress = 0L

                            val results = mutableListOf<Triple<Float, Float, Float>>()
                            for (i in 0 until Settings.count) {
                                generatorTimeTable.generateStartPopulation(Settings.countOfPopulation) {
                                    updateProgress(progress++, allProgress)
                                }
                                updateTitle("Генерация расписания")
                                generatorTimeTable.testExperiment {
                                    updateProgress(progress++, Settings.countCycle)
                                }.subscribe {
                                    val timetable = it.first
                                    val result = it.second
                                    results.add(result)
                                    generatorTimeTable.saveTimetable(timetable)
                                    Platform.runLater {
                                        Alert(AlertType.INFORMATION, "Расписание timetable.json создано в папке проекта", OK).apply {
                                            val stage = dialogPane.scene.window as Stage
                                            stage.icons.add(Image("/app/timetable.png"))
                                            stage.showAndWait()
                                        }
                                    }
                                    if (results.size == Settings.count) {
                                        Util.showResult(results)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Platform.runLater {
                                Alert(AlertType.ERROR, e.message, OK).apply {
                                    val stage = dialogPane.scene.window as Stage
                                    stage.icons.add(Image("/app/timetable.png"))
                                    stage.showAndWait()
                                }
                            }

                        }
                    }
                }

        controller.showViewOpenFile
                .subscribe {
                    val filters = arrayOf(FileChooser.ExtensionFilter("Файлы Excel", "*.xls", "*.xlsx"))
                    val files = chooseFile("Выберите файл", filters)
                    if (files.isNotEmpty()) {
                        runAsync {
                            updateTitle("Загрузка данных")
                            val generatorTimeTable = GeneratorTimetable(
                                    Settings.optimalLessonsOfDay,
                                    Settings.maxLessonsOfDay - 1,
                                    files[0]
                            )
                            val allProgress = (Settings.countOfPopulation + Settings.countCycle) * Settings.count
                            var progress = 0L

                            val results = mutableListOf<Triple<Float, Float, Float>>()

                            for (i in 0 until Settings.count) {
                                updateTitle("Генерация стартовой популяции")
                                var timeStart = Date().time
                                generatorTimeTable.generateStartPopulation(Settings.countOfPopulation) {
                                    updateProgress(progress++, allProgress)
                                }
                                var timeEnd = Date().time - timeStart
                                Util.printTime("Generation population of ${Settings.countOfPopulation} individual", timeEnd)
                                timeStart = Date().time
                                updateTitle("Генерация расписания")
                                generatorTimeTable.testExperiment {
                                    updateProgress(progress++, allProgress)
                                }.subscribe {
                                    timeEnd = Date().time - timeStart
                                    Util.printTime("Algorithm of ${Settings.countCycle} cycles", timeEnd)
                                    val timetable = it.first
                                    results.add(it.second)
                                    generatorTimeTable.saveTimetable(timetable)
                                    showTimetable(timetable, null)
                                    if (results.size == Settings.count) {
                                        Util.showResult(results)
                                    }
                                }
                            }
                        }
                    }
                }

        controller.openSettings
                .subscribe {
                    val newScope = Scope()
                    find<SettingsView>(newScope).openWindow(owner = null)
                }
    }

    private fun showTimetable(timetable: Timetable, groupNumber: String?) {
        val timetableForView = timetable.parseTimetableToView(groupNumber)
        if (timetableForView.first == null) {
            Platform.runLater {
                warning("Не найдена введенная группа")
            }
        } else {
            borderPane = borderpane {
                top {
                    form {
                        fieldset {
                            field("Группа:") {
                                groupNumberField = textfield(timetableForView.second ?: "")
                                button("Поиск").action {
                                    showTimetable(timetable, groupNumberField!!.text.toString())
                                }
                            }
                        }
                    }
                }
                center {
                    tableview(timetableForView.first) {
                        readonlyColumn("", GroupTimetableForView::time)
                        readonlyColumn("Понедельник", GroupTimetableForView::monday)
                        readonlyColumn("Вторник", GroupTimetableForView::tuesday)
                        readonlyColumn("Среда", GroupTimetableForView::wednesday)
                        readonlyColumn("Четверг", GroupTimetableForView::thursday)
                        readonlyColumn("Пятница", GroupTimetableForView::friday)
                        readonlyColumn("Суббота", GroupTimetableForView::saturday)
                    }.columns.forEach {
                        it.isSortable = false
                    }
                }
            }
        }
        Platform.runLater {
            showView()
        }
    }
}