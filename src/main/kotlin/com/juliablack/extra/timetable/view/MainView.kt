package com.juliablack.extra.timetable.view

import com.github.thomasnield.rxkotlinfx.actionEvents
import com.juliablack.extra.timetable.controller.EventController
import com.juliablack.extra.timetable.logic.genetic.GeneratorTimetable
import javafx.application.Platform
import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType
import javafx.scene.control.ButtonType.OK
import javafx.scene.image.Image
import javafx.scene.layout.BorderPane
import javafx.stage.FileChooser
import javafx.stage.Stage
import tornadofx.*

class MainView : View() {

    override val root = BorderPane()

    private val controller: EventController by inject()

    init {
        title = "ExtraTimeTable"
        showTopBar()
        subscribeAllEvent()
    }

    private fun showTopBar() {
        with(root) {
            setPrefSize(940.0, 610.0)
            top {
                this += menubar {
                    menu("Импорт") {
                        item("Из Excel-файла").apply {
                            actionEvents()
                                    .map { Unit }
                                    .subscribe(controller.showViewOpenFile)
                        }
                    }
                    menu("Сгенерировать") {
                        item("Из внутренней базы данных").apply {
                            actionEvents()
                                    .map { Unit }
                                    .subscribe(controller.generateTimetable)
                        }
                    }
                    menu("Настройки") {
                    }
                }
            }
            center {
                this += borderpane {
                    center {
                        imageview("/app/import_file.png") {
                            fitWidth = 390.0
                            fitHeight = 250.0
                        }
                    }
                }
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

                        val generatorTimeTable = GeneratorTimetable(OPTIONAL_LESSONS_OF_DAY, MAX_LESSONS_OF_DAY)
                        updateProgress(1, 3)
                        updateTitle("Генерация стартовой популяции")

                        try {
                            generatorTimeTable.generateStartPopulation(COUNT_OF_POPULATION)
                            updateProgress(2, 3)
                            updateTitle("Генерация расписания")
                            generatorTimeTable.generateTimetable().subscribe {
                                generatorTimeTable.saveTimetable(it)
                                Platform.runLater {
                                    Alert(AlertType.INFORMATION, "Расписание timetable.json создано в папке проекта", OK).apply {
                                        val stage = dialogPane.scene.window as Stage
                                        stage.icons.add(Image("/app/timetable.png"))
                                        stage.showAndWait()
                                    }
                                }
                            }
                            updateProgress(3, 3)
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
                            val generatorTimeTable = GeneratorTimetable(OPTIONAL_LESSONS_OF_DAY, MAX_LESSONS_OF_DAY, files[0])
                            updateProgress(1, 3)
                            updateTitle("Генерация стартовой популяции")

                        }
                    }

                }
    }

    companion object {
        const val COUNT_OF_POPULATION = 100
        const val MAX_LESSONS_OF_DAY = 6 //максимальное количество пар в день
        const val OPTIONAL_LESSONS_OF_DAY = 4 //желательное количество пар в день
    }
}