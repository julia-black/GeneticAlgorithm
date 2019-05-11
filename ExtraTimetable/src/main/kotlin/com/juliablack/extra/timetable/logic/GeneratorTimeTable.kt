package com.juliablack.extra.timetable.logic

import com.juliablack.geneticalgorithms.common.GeneticAlgorithm
import com.juliablack.geneticalgorithms.timetable.*
import com.juliablack.geneticalgorithms.timetable.enums.DayOfWeek
import com.juliablack.geneticalgorithms.timetable.enums.TypeLesson
import java.util.*


class GeneratorTimeTable {

    private lateinit var rooms: List<ClassRoom>
    private lateinit var lessons: List<Lesson>
    private lateinit var groups: List<Group>
    private lateinit var teachers: List<Teacher>
    private lateinit var studentProgram: List<GroupProgramm>

    private var listClasses: MutableList<StudentClassFull> = mutableListOf()

    private var geneticAlgorithm: GeneticAlgorithm

    init {
        downloadTimetableFromDB()
        geneticAlgorithm = TimetableGeneticAlg()
    }

    /**
     *  Генерация начальных расписаний в количестве count
     */
    fun generateStartPopulation(countIndividual: Int) {
        //todo:здесь будет составляться первые count раписаний, который будут составляться рандомно (но с учетом мин. требований)
        val population: MutableList<TimetableIndividual> = mutableListOf()

        for (i in 0 until countIndividual) {
            val individual = TimetableIndividual() //особь - одно расписание
            studentProgram.forEach { groupProgram ->
                groupProgram.lessons.forEach { lesson, count ->
                    for (j in 0 until count) {
                        val teacher = getTeacher(lesson)
                                ?: throw Exception("Не найдено преподавателя для предмета $lesson")
                        val room = getRandomRoom()
                        val time = getRandomTime(individual, room, 6)
                        individual.addItem(StudentClass(lesson, groupProgram.group, teacher), time, room)
                    }
                }
            }
            population.add(individual)
        }
        geneticAlgorithm.setStartPopulation(population)
    }

    private fun getTeacher(lesson: Lesson): Teacher? = teachers.find {
        it.lessons.contains(lesson)
    }

    private fun getRandomRoom(): ClassRoom = rooms[Random().nextInt(rooms.size)]


    private fun getRandomTime(timeTable: TimetableIndividual, room: ClassRoom, maxCountClasses: Int): Time {
        var time: Time
        do {
            val dayOfWeek = DayOfWeek.getRandomDay()
            val numberClass = Random().nextInt(maxCountClasses)
            time = Time(dayOfWeek, numberClass)
        } while (!isTimeFree(timeTable, time, room))
        return time
    }

    private fun isTimeFree(timeTable: TimetableIndividual, time: Time, room: ClassRoom): Boolean {
        //идем по всем
        timeTable.getClasses().forEach { studentClass ->
            //если в списке аудиторий уже есть такая
            timeTable.getRooms().getIndexes(room).forEachIndexed { indexRoom, _ ->
                if (timeTable.getTimes().getGen(indexRoom) == time
                        && timeTable.getClasses()[indexRoom] != studentClass)
                    return false
            }
        }
        return true
    }


    /**
     * Генерация расписания (основной процесс)
     */
    fun generateTimetable() {

    }

    /**
     * Загрузка из БД данных для составляения расписания
     */
    private fun downloadTimetableFromDB() {
        //todo:здесь будет загрузка из базы учителей, пар, групп, классов и т.д.

        val lesson = Lesson("Программирование", TypeLesson.LECTURE, false)
        val lesson1 = Lesson("Программирование", TypeLesson.PRACTICE, true)
        val lesson2 = Lesson("Анализ данных", TypeLesson.LECTURE, false)
        val lesson3 = Lesson("Математический анализ", TypeLesson.LECTURE, false)

        lessons = listOf(lesson, lesson1, lesson2, lesson3)

        val teacher1 = Teacher("Иванов И.И.", listOf(lesson, lesson1))
        val teacher2 = Teacher("Степанов С.С.", listOf(lesson2))
        val teacher3 = Teacher("Попова М.В.", listOf(lesson3))

        teachers = listOf(teacher1, teacher2, teacher3)

        val classRoom100 = ClassRoom(100, 12, 30, false, true)
        val classRoom101 = ClassRoom(101, 12, 50, false, true)
        val classRoom222 = ClassRoom(222, 12, 25, true, false)

        rooms = listOf(classRoom100, classRoom101, classRoom222)
        groups = listOf(Group(121, 20), Group(122, 23))

        studentProgram = listOf(
                GroupProgramm(groups[0], mapOf(Pair(lesson, 1), Pair(lesson1, 2), Pair(lesson2, 1), Pair(lesson3, 3))),
                GroupProgramm(groups[1], mapOf(Pair(lesson, 1), Pair(lesson1, 2), Pair(lesson3, 1)))
        )
    }
}