package org.uma.jmetal.problem.multiobjective;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.alg.color.*;
import org.jgrapht.alg.interfaces.VertexColoringAlgorithm.Coloring;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import org.uma.jmetal.problem.integermatrixproblem.impl.AbstractIntegerMatrixProblem;
import org.uma.jmetal.solution.integermatrixsolution.IntegerMatrixSolution;
import org.uma.jmetal.solution.integermatrixsolution.impl.DefaultIntegerMatrixSolution;

/**
 *
 * @author aadatti
 */
public final class MultiobjectiveETP_Compact extends AbstractIntegerMatrixProblem {

//    FIS fis;
    NumberFormat formatter = new DecimalFormat("#0000.00");

    Map<Integer, Student> studentMap;
    ArrayList<Exam> examVector;
    ArrayList<Exam> exclusiveExamsVector;
    ArrayList<Timeslot> timeslotVector;
    ArrayList<Room> roomVector;
    ArrayList<Department> departmentVector;
//    ArrayList<ArrayList<Integer>> timetableSolution;
//    ArrayList<ArrayList<Integer>> gspTimetableSolution;
    ArrayList<ArrayList<Integer>> compactTimetableSolution;
    ArrayList<Integer> largestExams;
//    ArrayList<Integer> courseType;
//    ArrayList<Integer> courseCredits;
//    ArrayList<Double> successRatio;
//    ArrayList<Integer> perceivedDifficulty;
//    ArrayList<Double> computedDifficulty;

    int numberOfExams;
    int numberOfTimeslots;
//    int numberOfCampuses;
//    int numberOfFaculties;
    int numberOfDepartments;
    int numberOfRooms;

    int twoInARowWeight;
    int twoInADayWeight;
    int periodSpreadWeight;
    int nonMixedDurationsWeight;
    int frontLoadWeight;
    int numberOfLargestExams;
    int numberOfLastPeriods;

    int spreadGap;

    private int examDuration;

    int[][] conflictMatrix;
    double[][] roomToRoomDistanceMatrix;
    double[][] departmentToRoomDistanceMatrix;

    int[][] exclusionMatrix;
    int[][] coincidenceMatrix;
    int[][] afterMatrix;

    boolean feasible;

    Graph<Integer, DefaultEdge> exGraph;
    Coloring coloredGraph;

    class Student {

        int sId, deptId;
        Integer room = null;
        ArrayList<Exam> examList = new ArrayList<>();

        Student(int id) {
            sId = id;
        }

        void addExam(Exam e) {
            examList.add(e);
        }
        
        void addDepartment(int d){
            deptId = d;
        }
        
        void setRoom(Exam e, Room r){
            for(Exam ex : examList){
                if(ex.examId==e.examId){
                    ex.setRoom(r.roomId);
                }
            }
        }
        
        void allocateRoom(int r){
            room = new Integer(r);
        }
        
        Exam getExam(int e){
            for(Exam ex:examList){
                if(ex.examId==e){
                    return ex;
                }
            }
            return null;
        }
    }

    class Exam {

        int examId, examDuration, priority, studentsCount = 0, dSizeToHCRatio, allocationCount, deAllocationCount;
        private double difficulty;
        boolean exlcusive = false;
        Timeslot timeslot;
        Room room;
        ArrayList<Room> roomsUsed;
        ArrayList<Student> enrollmentList;
        
        Map<Integer,Integer> studentRoomMap;
        
        Exam(int id, int duration) {
            this.enrollmentList = new ArrayList<>();
            this.roomsUsed = new ArrayList<>();
            this.studentRoomMap = new HashMap<>();
            examId = id;
            examDuration = duration;
        }
        
        void setGSPRoom(int student, int room){
            studentRoomMap.put(student, room);
        }

        void addStudent(Student student) {
            enrollmentList.add(student);
            studentsCount++;
        }

        void setTimeslot(int tSlot) {
            if (tSlot != -1) {
                allocationCount++;
                timeslot = timeslotVector.get(tSlot);               
            } else {
                deAllocationCount++;
                timeslot = null;                
            }
        }
                

        void setRoom(int i) {
            if (i != -1) {
                room = roomVector.get(i);
            } else {
                room = null;
            }
        }        
    }

    class Room {

        int freeSeats, capacity, roomId, penalty;
        double longitude, latitude, distanceToDept;        
        List<Exam> examList = new ArrayList<>();
        List<Student> studentList = new ArrayList<>();

        Room(int cap, int rId, double lon, double lat, int pen) {
            capacity = cap;
            roomId = rId;
//            myFaculty = facultyVector.get(fId);
            longitude = lon;
            latitude = lat;
            penalty = pen;
            freeSeats = capacity;
        }

        boolean allocateExam(int i) {
            Exam exam = examVector.get(i);
            if (!examList.contains(exam)) {
                int seats = getFreeSeats(exam.timeslot.id);
                if ( seats >= exam.studentsCount) {
                    examList.add(exam);
                    return true;
                }
                else{
                    System.out.println(seats+" SEATS INSUFFICIENT FOR "+exam.studentsCount+" STUDENTS");                    
                }
            }
            else{
                System.out.println("EXAM ALREADY ALLOCATED.");
            }
            return false;
        }
        
        void placeExamInRoom(int i){
            examList.add(examVector.get(i));
        }

        void deAllocateExam(int i) {
            Exam exam = examVector.get(i);
            if (!examList.remove(exam)) {
                System.out.println("-->deAllocating exam "+i+" failed<--");
            }
//            freeSeats+=exam.studentsCount;
        }

        int getFreeSeats(int timeslot) {
            int myFreeSeats = freeSeats;
            for (Exam e : getExams(timeslot)) {
                myFreeSeats -= e.studentsCount;
            }
            return myFreeSeats;
        }

        ArrayList<Exam> getExams(int timeslot) {
            ArrayList<Exam> result = new ArrayList();
            for (Exam e : examList) {
                if (e.timeslot.id == timeslot) {
                    result.add(e);
                }
            }
            return result;
        }
        
        void allocateStudent(Student std){
            studentList.add(std);
        }
        
        void setDistanceToDept(double lon, double lat){
            distanceToDept = gpsDistance(this.longitude,this.latitude,lon,lat);
        }
    }
    
    class Department{
        int deptId;
        double longitude, latitude;
        Map<Integer, ArrayList<Integer>> examStudentMap;
        
        Department(int dId, double lon, double lat){
            deptId = dId;            
            longitude = lon;
            latitude = lat;
            examStudentMap = new HashMap<>();            
            for(int i=0;i<numberOfExams;i++){
                examStudentMap.put(i, new ArrayList<>());  
            }
        }
    }

    class Timeslot {

        int id, duration, penalty;
        Date dateAndTime;
        ArrayList<Exam> examList = new ArrayList<>();

        Timeslot(int i, Date d, int dur, int pen) {
            id = i;
            dateAndTime = d;
            duration = dur;
            penalty = pen;
        }

        void addExam(int i) {
            Exam e = examVector.get(i);
            examList.add(e);
        }

        void removeExam(int i) {
            Exam e = examVector.get(i);
            if (examList.contains(e)) {
                examList.remove(e);
            }
        }
    }        
    
    class RoomComparatorByDistance implements Comparator<Room> {

        @Override
        public int compare(Room a, Room b) {            
            return a.distanceToDept < b.distanceToDept ? -1 : a.distanceToDept == b.distanceToDept ? 0 : 1;
        }
    } 

    public MultiobjectiveETP_Compact(String problemFile, String fuzzySystem, String examDifficultyData) throws IOException {
        studentMap = new HashMap<>();
        examVector = new ArrayList<>();
        exclusiveExamsVector = new ArrayList<>();
        timeslotVector = new ArrayList();
        roomVector = new ArrayList();
        departmentVector = new ArrayList();
//        timetableSolution = new ArrayList<>();
//        gspTimetableSolution = new ArrayList<>();
        compactTimetableSolution = new ArrayList<>();
//        largestExams = new ArrayList<>();
        spreadGap = 0;

        conflictMatrix = readProblem(problemFile);

        roomToRoomDistanceMatrix = new double[numberOfRooms][numberOfRooms];
        departmentToRoomDistanceMatrix = new double[numberOfDepartments][numberOfRooms];

        generateGSPDistanceMatrix();
//
//        System.out.println("Number of Students = "+studentMap.size());
//        System.out.println("Number of Exams = "+numberOfExams);
//        System.out.println("Number of Timeslots = "+numberOfTimeslots);
//        System.out.println("Number of Rooms = "+numberOfRooms);
//        System.out.println("Number of Campuses = "+numberOfCampuses);
//        System.out.println("Number of Faculties = "+numberOfFaculties);
        exGraph = new SimpleGraph<>(DefaultEdge.class);
        createGraph(conflictMatrix);

        setNumberOfVariables(numberOfExams);
        setNumberOfObjectives(1);
        setName("ETP");
    }

    @Override
    public int[][] getConflictMatrix() {
        return conflictMatrix;
    }   

    private int[][] readProblem(String file) throws IOException {
        InputStream in = getClass().getResourceAsStream(file);
        if (in == null) {
            in = new FileInputStream(file);
        }

        InputStreamReader isr = new InputStreamReader(in);
        BufferedReader br = new BufferedReader(isr);

        StreamTokenizer token = new StreamTokenizer(br);

        token.eolIsSignificant(true);
        boolean found = false;
//        found = false ;

        conflictMatrix = readExams(token, found);

        readEnrollmentByDepartment(token, found);
        readTimeslots(token, found);        
//        readCampuses(token, found);
//        readFaculties(token, found);
        readDepartments(token, found);
        readRooms(token, found);
        readTimeslotConstraints(token, found);
        readRoomConstraints(token, found);
        readWeightings(token, found);

        setExamPriorities();
        return conflictMatrix;
    }

    int[][] readExams(StreamTokenizer tok, boolean fnd) throws IOException {
        System.out.println("Reading Exams");
        tok.nextToken();
        while (!fnd) {
            if ((tok.sval != null) && ((tok.sval.compareTo("Exams") == 0))) {
                fnd = true;
            } else {
                tok.nextToken();
            }
        }

        tok.nextToken();
        tok.nextToken();

        numberOfExams = (int) tok.nval;
        
        tok.nextToken();
        tok.nextToken();
        tok.nextToken();

        addExam(tok);

        //Read Enrollments
        fnd = false;
        int t = 0;
        while (!fnd) {
            if ((tok.sval != null) && ((tok.sval.compareTo("EnrollmentByDepartment") == 0))) {
                tok.nextToken();
                tok.nextToken();
                numberOfDepartments = (int) tok.nval;
                System.out.println("Number of Exams = "+examVector.size());
                 System.out.println("Number of Departments = "+numberOfDepartments);
//                numberOfTimeslots = (int) tok.nval;
                fnd = true;
            } else {
                t = tok.nextToken();

                switch (t) {
                    case StreamTokenizer.TT_EOL:
                        tok.nextToken();
                        addExam(tok);
                        break;
                    case StreamTokenizer.TT_NUMBER:
                        int currentStudent = (int) tok.nval;
                        if (!studentMap.containsKey(currentStudent)) {
                            studentMap.put(currentStudent, new Student(currentStudent));
                        }
                        examVector.get(tok.lineno() - 2).addStudent(studentMap.get(currentStudent));
                        studentMap.get(currentStudent).examList.add(examVector.get(tok.lineno() - 2));
                        break;
                }
            }
        }

////        Print Student Map
//        for(Map.Entry<Integer,Student> entry : studentMap.entrySet())            
//        {
//            System.out.print("Student " + entry.getKey()+" Exams: ");
//            for(int i =0;i<entry.getValue().examList.size();i++)
//            {
//                System.out.print(entry.getValue().examList.get(i).examId+" ");
//            }
//            System.out.println();
//        }

//        System.out.println("List of Exams: ");
//        for(Exam ex:examVector){
//            System.out.println("Exam "+ex.examId);
//        }
        
        conflictMatrix = new int[numberOfExams][numberOfExams];

        //Generate Conflict Matrix
        ArrayList<Student> cleared = new ArrayList();
        int conflictCount=0;
        for (int currExam = 0; currExam < examVector.size() - 1; currExam++) {
            cleared.clear();
            int studentCount = examVector.get(currExam).enrollmentList.size();
            for (int currStudent = 0; currStudent < studentCount; currStudent++) {
                Student student = examVector.get(currExam).enrollmentList.get(currStudent);
                if (cleared.contains(student)) {
                    continue;
                }

                cleared.add(student);

                for (int nextExam = currExam + 1; nextExam < examVector.size(); nextExam++) {
                    if (examVector.get(nextExam).enrollmentList.contains(student)) {
                        conflictCount = conflictMatrix[currExam][nextExam];
                        conflictCount++;
                        conflictMatrix[currExam][nextExam] = conflictCount;
                        conflictMatrix[nextExam][currExam] = conflictCount;
                    }
                }
            }
        }

//        int Matrix ConflictMatrix;
        System.out.println("\nDISPLAYING int[][] Matrix CONFLICT MARIX:\n");
        for(int i=0;i<numberOfExams;i++)
        {
            for(int j=0;j<numberOfExams;j++)
            {
                System.out.print(conflictMatrix[i][j]+", ");
            }
            System.out.println();
        }

        System.out.println("///////////////////////////////////");
        System.out.println("Conflict Count = "+conflictCount);
        System.out.println("Elements of conflict Matrix = "+(numberOfExams*numberOfExams));
        double cDensity = new Double(conflictCount)/(numberOfExams*numberOfExams);
        System.out.println("Conflict Density = "+cDensity);
        System.out.println("///////////////////////////////////");
        
        return conflictMatrix;
    }

    void createGraph(int[][] cMat) {
        for (int v1 = 0; v1 < numberOfExams; v1++) {
            exGraph.addVertex(v1);
        }

        for (int v1 = 0; v1 < numberOfExams; v1++) {
            for (int v2 = 0; v2 < numberOfExams; v2++) {
                if (cMat[v1][v2] != 0) {
                    exGraph.addEdge(v1, v2);
                }
            }
        }
    }
    
    void readEnrollmentByDepartment(StreamTokenizer tok, boolean fnd) throws IOException{
        //Read EnrollmentByDepartment
        System.out.println("Reading Enrollment By Department");
        fnd = false;
        int t, student, deptCount = -1;
            
        while (!fnd) {
            if ((tok.sval != null) && ((tok.sval.compareTo("Periods") == 0))) {
                tok.nextToken();
                tok.nextToken();
                numberOfTimeslots = (int) tok.nval;
                System.out.println("Number of Timeslots = "+numberOfTimeslots);
//                System.out.println("Finished Reading EnrollmentByDepartment.");
                fnd = true;
            } else {
                t = tok.nextToken();
//                System.out.println("\tis "+t+" TT_EOL OR TT_NUMBER?");
                switch (t) {
                    case StreamTokenizer.TT_EOL:
//                        System.out.println("\t\t....TT_EOL");
                        deptCount++;
//                        System.out.println("Reading Next Dept..."+deptCount);
                        break;
                    case StreamTokenizer.TT_NUMBER:
//                        System.out.println("\t\t...TT_NUMBER");
                        student = (int) tok.nval;
//                        tok.nextToken();
//                        tok.nextToken();          
//                        System.out.println("\t\tReading Student "+student+" from dept "+deptCount);
                        if(!studentMap.containsKey(student))continue;
                        studentMap.get(student).deptId=deptCount;                        
                        break;
                }
            }
        }

//        System.out.println("Enrollment Count By Department");
//        for(int i=0; i<timeslotVector.size();i++)
//        {
//            Timeslot timeS = timeslotVector.get(i);
//            System.out.print("Timeslot "+timeS.id);
//            System.out.print(" @ "+timeS.dateAndTime.toLocaleString());
////            System.out.print(" at time "+timeS.getTime());
//            System.out.println(" has "+timeS.duration+" minutes");
//        }
    }

    void readTimeslots(StreamTokenizer tok, boolean fnd) throws IOException {
        //Read Timeslots
        System.out.println("Reading Timeslots");
        fnd = false;
        int t, tCount = 0;
//        int day, pos, duration, penalty;
//        day = pos = duration = 0;
        int day, month, year, hour, minutes, seconds, duration, penalty;
        day = month = year = hour = minutes = seconds = duration = penalty = 0;
        while (!fnd) {
            if ((tok.sval != null) && ((tok.sval.compareTo("Departments") == 0))) {
                tok.nextToken();
                tok.nextToken();
//                numberOfDepartments = (int) tok.nval;
//                System.out.println("Number of Departments = "+ numberOfCampuses);
//                System.out.println("Finished Reading Timeslots.");
                fnd = true;
            } else {
                t = tok.nextToken();

                switch (t) {
                    case StreamTokenizer.TT_EOL:
                        break;
                    case StreamTokenizer.TT_NUMBER:
//                        day = (int) tok.nval;tok.nextToken();tok.nextToken();                                                
//                        pos = (int) tok.nval;tok.nextToken();tok.nextToken();
//                        duration = (int) tok.nval;tok.nextToken();tok.nextToken();
//                        penalty = (int) tok.nval;tok.nextToken();

                        day = (int) tok.nval;
                        tok.nextToken();
                        tok.nextToken();
                        month = (int) tok.nval;
                        tok.nextToken();
                        tok.nextToken();
                        year = (int) tok.nval;
                        tok.nextToken();
                        tok.nextToken();
                        hour = (int) tok.nval;
                        tok.nextToken();
                        tok.nextToken();
                        minutes = (int) tok.nval;
                        tok.nextToken();
                        tok.nextToken();
                        seconds = (int) tok.nval;
                        tok.nextToken();
                        tok.nextToken();
                        duration = (int) tok.nval;
                        tok.nextToken();
                        tok.nextToken();
                        penalty = (int) tok.nval;
                        tok.nextToken();
//                        System.out.println(day+":"+month+":"+year+", "+hour+":"+minutes+":"+seconds+", "+duration+", "+penalty);
                        Date examDateAndTime = new Date(year - 1900, month - 1, day, hour, minutes, seconds);
//                        System.out.println("examDateAndTime = "+examDateAndTime.toLocaleString());
                        addTimeslot(tCount++, examDateAndTime, duration, penalty);
                        break;
                }
            }
        }

//        System.out.println("Timeslots Vector");
//        for(int i=0; i<timeslotVector.size();i++)
//        {
//            Timeslot timeS = timeslotVector.get(i);
//            System.out.print("Timeslot "+timeS.id);
//            System.out.print(" @ "+timeS.dateAndTime.toLocaleString());
////            System.out.print(" at time "+timeS.getTime());
//            System.out.println(" has "+timeS.duration+" minutes");
//        }
    }
       
    void readDepartments(StreamTokenizer tok, boolean fnd) throws IOException {
        //Read Departments
        System.out.println("Reading Departments");
        fnd = false;
        int t, dCount = 0;
        double lon = 0.0, lat = 0.0;
        while (!fnd) {
            if ((tok.sval != null) && ((tok.sval.compareTo("Rooms") == 0))) {
                tok.nextToken();
                tok.nextToken();
//                System.out.println("Finished Reading Departments.");
                numberOfRooms = (int) tok.nval;
//                System.out.println("Number of Rooms = "+numberOfRooms);
                fnd = true;
            } else {
                t = tok.nextToken();
                switch (t) {
                    case StreamTokenizer.TT_EOL:
                        break;
                    case StreamTokenizer.TT_NUMBER:                        
                        lon = tok.nval;
                        tok.nextToken();
                        tok.nextToken();
                        lat = tok.nval;
                        tok.nextToken();
                        addDepartment(dCount++, lon, lat);
                        break;
                }
            }
        }
        
//        System.out.println("Departments Vector");
//        for(int i=0; i<departmentVector.size();i++)
//        {
//            System.out.print("Department "+departmentVector.get(i).deptId+" is");            
//            System.out.print(" at longitude "+departmentVector.get(i).longitude);
//            System.out.print(" and latitude "+departmentVector.get(i).latitude+" is ");        
//        }
    }

    void readRooms(StreamTokenizer tok, boolean fnd) throws IOException {
        //Read Rooms
        System.out.println("Reading Rooms");
        fnd = false;
        int t, rCount = 0, cap, penalty;
        double longitude=0.0, latitude=0.0;
        while (!fnd) {
            if ((tok.sval != null) && ((tok.sval.compareTo("PeriodHardConstraints") == 0))) {
                tok.nextToken();
                tok.nextToken();
//                System.out.println("Finished Reading Rooms.");
                fnd = true;
            } else {
                t = tok.nextToken();
                switch (t) {
                    case StreamTokenizer.TT_EOL:
                        break;
                    case StreamTokenizer.TT_NUMBER:
                        cap = (int) tok.nval;
                        tok.nextToken();
                        tok.nextToken();

                        longitude = (double) tok.nval;
                        tok.nextToken();
                        tok.nextToken();

                        latitude = (double) tok.nval;
                        tok.nextToken();
                        tok.nextToken();

                        penalty = (int) tok.nval;
                        tok.nextToken();

                        addRoom(cap, rCount++, longitude, latitude, penalty);
                        break;
                }
            }
        }

//        System.out.println("Room Vector");
//        for(int i=0; i<roomVector.size();i++)
//        {
//            System.out.print("Room "+roomVector.get(i).roomId);
//            System.out.print(" in faculty "+roomVector.get(i).myFaculty.facId);
//            System.out.print(" has capacity "+roomVector.get(i).capacity);
//            System.out.println(" and is "+roomVector.get(i).distToFaculty+"m from main faculty entrance.");         
//        }
    }

    void readTimeslotConstraints(StreamTokenizer tok, boolean fnd) throws IOException {
        exclusionMatrix = new int[numberOfExams][numberOfExams];
        coincidenceMatrix = new int[numberOfExams][numberOfExams];
        afterMatrix = new int[numberOfExams][numberOfExams];
//        System.out.println("Reading PeriodHardConstraints");
        //Read PeriodHardConstraints
        tok.wordChars('_', '_');
        fnd = false;
        int t;
        while (!fnd) {
            if ((tok.sval != null) && ((tok.sval.compareTo("RoomHardConstraints") == 0))) {
                tok.nextToken();
                tok.nextToken();
//                numberOfRooms=(int)tok.nval;
//                System.out.println("Finished Reading PeriodHardConstraints.");
                fnd = true;
            } else {
                t = tok.nextToken();
                int exam1 = -1, exam2 = -1;
                String constraint = "";
                switch (t) {
                    case StreamTokenizer.TT_EOL:
                        break;
                    case StreamTokenizer.TT_NUMBER:
                        //System.out.println("nextToken():"+tok.nval);
                        exam1 = (int) tok.nval;//System.out.println("exam1:"+exam1);
                        tok.nextToken();
                        tok.nextToken();
                        constraint = tok.sval;//System.out.println("constraint:"+constraint);
                        tok.nextToken();
                        tok.nextToken();
                        exam2 = (int) tok.nval;//System.out.println("exam2:"+exam2);
                        break;
//                    case StreamTokenizer.TT_WORD:
//                        //System.out.println("nextToken():"+tok.sval);
//                        break;
                }

                switch (constraint) {
                    case "EXCLUSION":
//                        exclusionConstraintVector.put(exam1, exam2);
                        exclusionMatrix[exam1][exam2] = 1;
                        break;
                    case "EXAM_COINCIDENCE":
//                        coincidenceConstraintVector.put(exam1, exam2);
                        coincidenceMatrix[exam1][exam2] = 1;
                        break;
                    case "AFTER":
//                        afterConstraintVector.put(exam1, exam2);
                        afterMatrix[exam1][exam2] = 1;
                        break;
                }
            }
        }

//        System.out.println("exclusionMatrix:"+Arrays.deepToString(exclusionMatrix));
//        System.out.println("coincidenceMatrix:"+Arrays.deepToString(coincidenceMatrix));
//        System.out.println("afterMatrix:"+Arrays.deepToString(afterMatrix));
    }

    void readRoomConstraints(StreamTokenizer tok, boolean fnd) throws IOException {
        //Read RoomHardConstraints
//        System.out.println("Reading Room Constraints");
        fnd = false;
        int t;
        while (!fnd) {
            if ((tok.sval != null) && ((tok.sval.compareTo("InstitutionalWeightings") == 0))) {
                tok.nextToken();
                tok.nextToken();
                fnd = true;
//                System.out.println("Finished Reading Room Constraints");
            } else {
                t = tok.nextToken();
                switch (t) {
                    case StreamTokenizer.TT_EOL:
                        break;
                    case StreamTokenizer.TT_NUMBER:
//                        System.out.println("nextToken():"+tok.nval);
                        examVector.get((int) tok.nval).exlcusive = true;
                        exclusiveExamsVector.add(examVector.get((int) tok.nval));
                        break;
                }
            }
        }
//        System.out.println("exclusiveExamsVector:");
//        for(int i=0;i<examVector.size();i++)
//        {
//            System.out.println(examVector.get(i).examId+"--> "+examVector.get(i).exlcusive);
//        }
    }

    void readWeightings(StreamTokenizer tok, boolean fnd) throws IOException {
        //Read InstitutionalWeightings
//        System.out.println("Reading Weightings");
        int t = tok.nextToken();
        while (t != StreamTokenizer.TT_EOF) {
            switch (t) {
                case StreamTokenizer.TT_EOL:
                    break;
                case StreamTokenizer.TT_WORD:
//                    System.out.println("nextToken():"+tok.sval);
                    if (tok.sval.compareTo("TWOINAROW") == 0) {
                        tok.nextToken();
                        tok.nextToken();
                        twoInARowWeight = (int) tok.nval;
                    } else if (tok.sval.compareTo("TWOINADAY") == 0) {
                        tok.nextToken();
                        tok.nextToken();
                        twoInADayWeight = (int) tok.nval;
                    } else if (tok.sval.compareTo("PERIODSPREAD") == 0) {
                        tok.nextToken();
                        tok.nextToken();
                        periodSpreadWeight = (int) tok.nval;
                    } else if (tok.sval.compareTo("NONMIXEDDURATIONS") == 0) {
                        tok.nextToken();
                        tok.nextToken();
                        nonMixedDurationsWeight = (int) tok.nval;
                    } else if (tok.sval.compareTo("FRONTLOAD") == 0) {
                        tok.nextToken();
                        tok.nextToken();
                        numberOfLargestExams = (int) tok.nval;
                        tok.nextToken();
                        tok.nextToken();
                        numberOfLastPeriods = (int) tok.nval;
                        tok.nextToken();
                        tok.nextToken();
                        frontLoadWeight = (int) tok.nval;
                    }
                    break;
            }
            t = tok.nextToken();
        }
//        System.out.println("twoinarow:"+twoInARowWeight);
//        System.out.println("twoinaday:"+twoInADayWeight);
//        System.out.println("periodSpread:"+periodSpreadWeight);
//        System.out.println("nonMixedDurations:"+nonMixedDurationsWeight);
//        System.out.println("numberOfLargestExams:"+numberOfLargestExams);
//        System.out.println("numberOfLastPeriods:"+numberOfLastPeriods);
//        System.out.println("frontLoadPenalty:"+frontLoadWeight);
    }

    void setExamPriorities() {
        for (Exam e : examVector) {
            e.priority = numberOfTimeslots;
        }

        for (int i = 0; i < numberOfExams; i++) {
            for (int j = 0; j < numberOfExams; j++) {
                if (i == j) {
                    continue;
                }
                if (afterMatrix[i][j] == 1) {
                    int priority = examVector.get(j).priority;
                    priority -= 1;
                    examVector.get(j).priority = priority;
                }
            }
        }
    }
    
    void generateGSPDistanceMatrix(){
        for(int d=0;d<numberOfDepartments;d++){
           double dLon = departmentVector.get(d).longitude;
           double dLat = departmentVector.get(d).latitude;
           for(int r = 0;r<numberOfRooms;r++){
               double rLon = roomVector.get(r).longitude;
               double rLat = roomVector.get(r).latitude;
               departmentToRoomDistanceMatrix[d][r]=gpsDistance(dLon,dLat,rLon,rLat);
           }
       }
        
        //Display departmentToRoomDistanceMatrix
//        System.out.println("departmenToRoomDistanceMatrix:");
//        for(int j=0;j<numberOfRooms;j++)System.out.print("\t\tRoom: "+j);
//        System.out.println();
//        for(int i=0;i<numberOfDepartments;i++){
////            System.out.print("Department: "+i);
//            for(int j=0;j<numberOfRooms;j++){
//                System.out.print("\t"+formatter.format(departmentToRoomDistanceMatrix[i][j])+" ");
//            }
//            System.out.println();
//        }
    }

    void generateDistanceMatrices() {
        for (int i = 0; i < numberOfRooms; i++) {
            for (int j = i; j < numberOfRooms; j++) {
                double long1, long2, lat1, lat2;
                Room rm1 = roomVector.get(i);
                Room rm2 = roomVector.get(j);

                long1 = rm1.longitude;
                lat1 = rm1.latitude;
                long2 = rm2.longitude;
                lat2 = rm2.latitude;
                
                roomToRoomDistanceMatrix[i][j] = gpsDistance(long1, lat1, long2, lat2);
                roomToRoomDistanceMatrix[j][i] = roomToRoomDistanceMatrix[i][j];
            }
        }
        
        //Display roomToRoomDistanceMatrix
//        System.out.println("roomToRoomDistanceMatrix: ");
//        for(int i=0;i<numberOfRooms;i++)
//        {
//            for(int j=0;j<numberOfRooms;j++)
//            {
//                System.out.print(formatter.format(roomToRoomDistanceMatrix[i][j])+" ");
//            }
//            System.out.println();
//        }
    }

    double gpsDistance(double lo1, double la1, double lo2, double la2) {
        double R = 6378.137; // Radius of earth in KM
        double dLat = la2 * Math.PI / 180 - la1 * Math.PI / 180;
        double dLon = lo2 * Math.PI / 180 - lo1 * Math.PI / 180;
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(la1 * Math.PI / 180) * Math.cos(la2 * Math.PI / 180)
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double d = R * c;
        return d; // d * 1000 meters          
    }

    void addExam(StreamTokenizer tok) {
        int line = tok.lineno() - 2;
        examDuration = (int) tok.nval;
        if (line < numberOfExams) {
            examVector.add(new Exam(line, examDuration));
        }
    }

    //tCount, examDateAndTime, duration, penalty
//    void addTimeslot(int id, int d, int p, int dur, int pen) {
    void addTimeslot(int id, Date date, int dur, int pen) {
        timeslotVector.add(new Timeslot(id, date, dur, pen));
    }

    void addRoom(int c, int id, double lon, double lat, int pen) {
        roomVector.add(new Room(c, id, lon, lat, pen));
//        overallRoomCapacity+=c;
    }
    
    void addDepartment(int id, double lon, double lat){
        departmentVector.add(new Department(id,lon,lat));
    }       

    @Override
    public IntegerMatrixSolution<ArrayList<Integer>> createSolution() {
        String heuristic = "CLOSEST_FIRST";
//        String heuristic = "RANDOM";
        
        return allocateGSP(heuristic);
    }
    
    IntegerMatrixSolution allocateGSP(String heuristic){
        System.out.println("\nNew Solution using "+heuristic+" heuristic.");
        IntegerMatrixSolution solution = new DefaultIntegerMatrixSolution(getListOfExamsPerVariable(), getNumberOfObjectives()); 
        PriorityQueue<Room> sortedRooms = new PriorityQueue(numberOfRooms, new RoomComparatorByDistance());
//        ArrayList<Integer> randTimeslots = new ArrayList<>();        
//        ArrayList<Integer> tmpStudents = new ArrayList<>();
        ArrayList<Integer> randExams = new ArrayList<>();

//        for(int t=0;t < numberOfTimeslots;t++)randTimeslots.add(t);
//        Collections.shuffle(randTimeslots);
        
//        for (int i = 0; i < studentMap.size(); i++)tmpStudents.add(i, -1);
        
//        gspTimetableSolution.clear();
        compactTimetableSolution.clear();
//        for (int j = 0; j < examVector.size(); j++)gspTimetableSolution.add(j, new ArrayList<>(tmpStudents));
        for (int j = 0; j < 3; j++)compactTimetableSolution.add(j, new ArrayList<>());
        
        for(int e=0;e<numberOfExams;e++)randExams.add(e);
        Collections.shuffle(randExams);
        
//        System.out.println("EXAM ENROLLMENT BY DEPARTMENT");
        for(Exam ex:examVector){    
//            System.out.println("Exam "+ex.examId);
            for(Department dept : departmentVector){    
//                int count=0;                
                dept.examStudentMap.get(ex.examId).clear();
//                System.out.print("\n\tDepartment "+dept.deptId+". Student List : \n\n");
                for(Student student:ex.enrollmentList){ 
//                    System.out.println("\t\tStudent "+student.sId+" from dept "+student.deptId);
                    if(dept.deptId==student.deptId){
//                        count++;
                        dept.examStudentMap.get(ex.examId).add(student.sId);                        
//                        System.out.print(student.sId+", ");
                    }                    
                }                
//                System.out.println("\tStudents from dept "+dept.deptId+" = "+count);
            }
        }
                
        
        
//        System.out.println("NUMBER OF EXAMS: "+examVector.size());             
//        int colIndex=0;
        while(randExams.size()>0){   
//            System.out.println("Unallocated randExams.size() : "+randExams.size());
            Exam exam = examVector.get(randExams.get(0));
            randExams.remove(0);
            int examId = exam.examId;
            int enrollment = exam.enrollmentList.size();
            
//            System.out.print("\nNext Exam "+exam.examId+"..."+"\tEnrollment: "+enrollment+"\n");               
            
//            Timeslot timeSlot;
//            if(randTimeslots.size()>0){
//                int tSlot = randTimeslots.get(0);
//                randTimeslots.remove(0);
//                exam.setTimeslot(tSlot);
//                timeSlot = timeslotVector.get(tSlot);
//                timeSlot.addExam(examId);                
//            }else{
//                System.out.println("\t\tError: Timeslots Exhausted.");
//                break;
//            } 
//            System.out.println("\tClearing student list from rooms...");
            for(Room rm:roomVector){
                rm.studentList.clear();
//                System.out.println("\t\tRoom "+rm.roomId+" = "+rm.studentList.size());
            }
            
            
            if("RANDOM".equals(heuristic)){
                ArrayList<Room> availableRooms = new ArrayList<>();        
                availableRooms.addAll(roomVector);
                Collections.shuffle(availableRooms);
                
                ArrayList<Integer> randStudent = new ArrayList<>();
                for(int i=0;i<enrollment;i++)randStudent.add(i);
                Collections.shuffle(randStudent);
                
    //            int rm=0;
                int stud=0;
                System.out.println("\t\tAVAILABLE ROOMS: "+availableRooms.size());
                while(availableRooms.size()>0){
                    Room currRoom = availableRooms.get(0);
    //                currRoom.placeExamInRoom(exam.examId);                 
                    while(stud<enrollment){
                        Student currStudent = exam.enrollmentList.get(randStudent.get(stud));                                                   
                        if(currRoom.studentList.size()<currRoom.capacity){                       
                            currRoom.allocateStudent(currStudent);
    //                        currStudent.setRoom(exam, currRoom);
    //                        exam.roomsUsed.add(currRoom);
                            exam.setGSPRoom(currStudent.sId-1, currRoom.roomId);
//                            gspTimetableSolution.get(examId).set(currStudent.sId-1, currRoom.roomId);                        
//                            solution.setVariable(examId, gspTimetableSolution.get(examId));
                            
                            compactTimetableSolution.get(0).add(examId);
                            compactTimetableSolution.get(1).add(currStudent.sId);
                            compactTimetableSolution.get(2).add(currRoom.roomId);
                            
//                            solution.setVariable(0, compactTimetableSolution.get(0)); 
//                            solution.setVariable(1, compactTimetableSolution.get(1)); 
//                            solution.setVariable(2, compactTimetableSolution.get(2)); 
                            
//                            colIndex++;
    //                        System.out.println("gspTimetableSolution("+examId+")"+gspTimetableSolution.get(examId));
    //                        System.out.println("solution.getVariable("+examId+") = "+solution.getVariable(examId));
    //                        currStudent.getExam(exam.examId).setRoom(currRoom.roomId);
//                            System.out.println("\t\t\tStudent "+currStudent.sId+" is in room "+exam.studentRoomMap.get(currStudent.sId-1));
                            stud++;
                        }else{
//                            System.out.println("\t\t\tRoom "+currRoom.roomId+" filled. "+(availableRooms.size()-1)+" rooms remaining. Moving to next Room");
                            availableRooms.remove(currRoom);
                            break;
                        }
                    }
                    if(stud>=enrollment)break;
                }
                if(availableRooms.size()<=0){//System.out.println("\t\t\tRooms Exhausted. Failed.");
                    break;
                }
            }else if("CLOSEST_FIRST".equals(heuristic)){            
                sortedRooms.clear();
//                System.out.println("\tSorted Rooms Cleared. Size = "+sortedRooms.size());
                sortedRooms.addAll(roomVector);
//                System.out.println("\tSorted Rooms Populated. Size = "+sortedRooms.size());

                ArrayList<Room> roomsInList = new ArrayList<>();
                roomsInList.addAll(sortedRooms);
//                System.out.println("\tPopulating roomsInList from sortedRooms");
//                int size = sortedRooms.size();
//                for(int i=0;i<size;i++){
//                    Room head = sortedRooms.poll();
////                    System.out.println("\tPolling "+head.roomId+" into index "+i);
//                    roomsInList.add(i,head);
//                }

                for(Department dept : departmentVector){
//                    System.out.print("\n\tExam "+examId+"\tDepartment "+dept.deptId);
//                    System.out.println("\tStudents Count = "+dept.examStudentMap.get(examId).size()+"\t\t");                
//                    int count=0;
//                    for(Student student:exam.enrollmentList){          
//                        if(dept.deptId==student.deptId){
//                            count++;
////                            System.out.print(student.sId+", ");
//                            dept.examStudentMap.get(examId).add(student.sId);
//                        }
//                    }
//                    System.out.println("\tTotal students = "+count);

//                    System.out.println("\t"+roomsInList.size()+" rooms available. Capacities & Free Seats");
                    for(Room rm:roomsInList){
//                        System.out.println("\t\tRoom "+rm.roomId+"("+rm.capacity+"). Free Seats = "+(rm.capacity-rm.studentList.size()));
                        rm.setDistanceToDept(dept.longitude, dept.latitude);
                    }

                    
                    PriorityQueue<Room> closeRooms = new PriorityQueue<>(numberOfRooms,new RoomComparatorByDistance());
                    closeRooms.addAll(roomsInList);
                    ArrayList<Room> roomQ = new ArrayList<>(numberOfRooms);
                    int sortedRoomsCount = closeRooms.size();
                    for(int i=0;i<sortedRoomsCount;i++){
                        Room head = closeRooms.poll();
    //                    System.out.println("\tPolling "+head.roomId+" into index "+i);
                        roomQ.add(i,head);
                    }
//                    System.out.println("\n\troomQ: size() "+roomQ.size());
//                    int q=0;
//                    while(q<roomQ.size()){
////                        Room closeRoom = closeRooms.poll();
//                        System.out.println("\t\tRoom "+roomQ.get(q++).roomId+", distance: "+roomQ.get(q++).distanceToDept);
//                    }
                    int allocatedCount=0;
                    for(Integer stdId:dept.examStudentMap.get(examId)){                    
                        Student currentStudent = studentMap.get(stdId);
//                        System.out.print("\t\tNext Student "+currentStudent.sId+" dept = "+dept.deptId);
                        ////////////////////////////////////
                        while(!roomQ.isEmpty()){
                            Room currentRoom = roomQ.get(0);
                            if(currentRoom.studentList.size()<currentRoom.capacity){
                                exam.setGSPRoom(currentStudent.sId-1, currentRoom.roomId);
//                                System.out.println("\t\t\tPlacing Student "+stdId+" in curr room "+currentRoom.roomId+" ("+currentRoom.distanceToDept+" meters)");                       

                                compactTimetableSolution.get(0).add(examId);
                                compactTimetableSolution.get(1).add(stdId);
                                compactTimetableSolution.get(2).add(currentRoom.roomId);
                                allocatedCount++;
                                break;
                            }
                            else{
                                roomQ.remove(currentRoom);                                
                            }
                        }
                        
//                        if(allocatedCount!=dept.examStudentMap.get(examId).size()){                            
//                            System.out.print("\t\t\tFAILED! Cant allocate all students. Rooms exhausted. ");
//                            System.out.println(allocatedCount+" != "+dept.examStudentMap.get(examId).size());
//                        }
                        ////////////////////////////////////
//                        Room currentRoom = roomQ.get(0);
////                        System.out.println("\tRoom "+currentRoom.roomId);
//                        if(currentRoom.studentList.size()<currentRoom.capacity){                       
//                            currentRoom.allocateStudent(currentStudent);
//                            exam.setGSPRoom(currentStudent.sId-1, currentRoom.roomId);
//                            System.out.println("\t\t\tPlacing Student "+stdId+" in curr room "+currentRoom.roomId+" ("+currentRoom.distanceToDept+" meters)");
////                            gspTimetableSolution.get(examId).set(currentStudent.sId-1, currentRoom.roomId);                        
////                            solution.setVariable(examId, gspTimetableSolution.get(examId));                            
//                            
//                            compactTimetableSolution.get(0).add(examId);
//                            compactTimetableSolution.get(1).add(stdId);
//                            compactTimetableSolution.get(2).add(currentRoom.roomId);
//                            
////                            solution.setVariable(0, compactTimetableSolution.get(0)); 
////                            solution.setVariable(1, compactTimetableSolution.get(1)); 
////                            solution.setVariable(2, compactTimetableSolution.get(2)); 
//                            
////                            colIndex++;
//                        }else{
////                            System.out.println("\t\t\tRoom "+currentRoom.roomId+" filled. Removing from sortedRooms.");
//                            roomQ.remove(currentRoom); 
//                            if(roomQ.isEmpty()){
////                              System.out.println("\tRooms Exhausted");
//                                break;
//                            }
//                            else{
//                                currentRoom = roomQ.get(0);
//                                if(currentRoom.studentList.size()<currentRoom.capacity){                       
//                                    currentRoom.allocateStudent(currentStudent);
//                                    exam.setGSPRoom(currentStudent.sId-1, currentRoom.roomId);
//                                    System.out.println("\t\t\tPlacing Student "+stdId+" in next room "+currentRoom.roomId+" ("+currentRoom.distanceToDept+" meters)");
////                                    gspTimetableSolution.get(examId).set(currentStudent.sId-1, currentRoom.roomId);                        
////                                    solution.setVariable(examId, gspTimetableSolution.get(examId));
//                                    
//                                    compactTimetableSolution.get(0).add(examId);
//                                    compactTimetableSolution.get(1).add(stdId);
//                                    compactTimetableSolution.get(2).add(currentRoom.roomId);
//
////                                    solution.setVariable(0, compactTimetableSolution.get(0)); 
////                                    solution.setVariable(1, compactTimetableSolution.get(1)); 
////                                    solution.setVariable(2, compactTimetableSolution.get(2)); 
//
////                                    colIndex++;
//                                }
//                                else{
//                                    System.out.println("\t\t\t****Last Room filled. No more rooms****");                                    
//                                }
//                            }
//                        }
                    }
                }                        
            }else{
                System.out.println("Invalid Heuristic. Please chose one of 'RANDOM' or 'CLOSEST_FIRST'");
            }  
//            System.out.println("Finised Placing Exam "+examId+" to rooms.");
//            System.out.println("Exam ArrayList = "+gspTimetableSolution.get(examId));
        }
        
        
//        System.out.println("TIMETABLE:");
//        for(Exam exam:examVector){
//            System.out.print("Exam : "+exam.examId+". Enrollment ("+exam.studentsCount+")");
//            if(exam.timeslot == null){
////                System.out.println(". Timeslot = Unassinged.");
//            }
//            else{
////                System.out.println();
////                System.out.println("\tTimeslot : "+exam.timeslot.dateAndTime.toGMTString());
//            }            
////            System.out.println("\tVenues : ");
//            
//            for(int r=0; r<numberOfRooms;r++){
////                System.out.println("\t\tRoom "+r);
//                int sCount=0;
//                for(Integer student:exam.studentRoomMap.keySet()){
//                    if(exam.studentRoomMap.get(student)==r){
////                        System.out.println("\t\t\tStudent "+student+" from department "+studentMap.get(student+1).deptId);
//                        sCount++;
//                    }                                         
//                }
////                if(sCount!=0)System.out.println("\t\t\tRoom "+r+". Total students = "+sCount);
//            }
//        }
        
//        for (int i = 0; i < getLength(); i++) {            
        for (int i = 0; i < 3; i++) {                        
            solution.setVariable(i, compactTimetableSolution.get(i));
            
//            System.out.println(solution.getVariable(i));
        }
        
//        int exCount = Collections.max(compactTimetableSolution.get(0));
//        int stdCount = Collections.max(compactTimetableSolution.get(1));
//        System.out.println("Max Exams = "+exCount);
//        System.out.println("Max Stundet = "+stdCount);
        
        System.out.println("Datti\nFinal Allocation:\nStudent,Exam,Room,Dept,Distance");//+gspTimetableSolution.size());                
        for(int i=0;i<compactTimetableSolution.get(0).size();i++){
//            System.out.println("\nExam "+i+":");            
            int exam = compactTimetableSolution.get(0).get(i);
            int student = compactTimetableSolution.get(1).get(i);
            int dept = studentMap.get(student).deptId;
            int room = compactTimetableSolution.get(2).get(i);
            System.out.println(student+",\t"+exam+",\t"+room+",\t"+dept+",\t"+departmentToRoomDistanceMatrix[dept][room]);            
        }

        return solution;
    }    

    @Override
    public void evaluate(IntegerMatrixSolution<ArrayList<Integer>> solution) {
//        double proximityFitness = evaluateProximityFitness(solution);
//        double movementFitness = evaluateMovementFitness(solution);                 //movement cost
//        double roomUtilizationFitness = evaluateRoomUtilizationFitness(solution);   //room cost
//        double gspRoomUtilizationFitness = evaluateGspRoomUtilizationFitness(solution);   //gspRoom cost
        double gspMovementFitness = evaluateGSPMovementFitness(solution);

//        double compositeFitness = proximityFitness + movementFitness + roomUtilizationFitness;
//        int itc2007Fitness = evaluateITC2007Fitness(solution);

//        this.evaluateConstraints(solution);
//        solution.setObjective(0, proximityFitness);
        solution.setObjective(0, gspMovementFitness);
//        solution.setObjective(1, gspRoomUtilizationFitness); 
//        solution.setObjective(0, compositeFitness);
//        solution.setObjective(0, itc2007Fitness);

//        System.out.println("\nFINAL SOLUTION:");
//        for(int i=0;i<solution.getNumberOfVariables();i++){
//            System.out.println("Solution.getVariables() = "+solution.getVariables());
//        }
        
//closest        System.out.println("Objective_1 (gspRoomUtilizationFitness) = " + solution.getObjective(1));// + " Conflicts = " + solution.getAttribute("CONFLICTS"));
        System.out.println("solution Objective = "+solution.getObjective(0));// + " Conflicts = " + solution.getAttribute("CONFLICTS"));        
//        System.out.println("\tCONFLICTS = " + solution.getAttribute("CONFLICTS"));//+"\tObjective(1) =" + solution.getObjective(1)+"\tObjective(0) =" + 
//                solution.getObjective(0));                
    }

//    public double evaluateProximityFitness(IntegerMatrixSolution<ArrayList<Integer>> solution) {
//        //proximity constraint 
//        double proximity = 0.0;
//        for (int i = 0; i < solution.getNumberOfVariables(); i++) {
//            int slot1 = getTimeslot(solution.getVariable(i));
//            if (slot1 == -1) {
//                return Integer.MAX_VALUE;
//            }
//            for (int j = 0; j < solution.getNumberOfVariables(); j++) {
//                if (i == j) {
//                    continue;
//                }
//                int slot2 = getTimeslot(solution.getVariable(j));
//                if (slot2 == -1) {
//                    return Integer.MAX_VALUE;
//                }
//                if (conflictMatrix[i][j] != 0) {
//                    double prox = Math.pow(2, (5 - Math.abs(slot1 - slot2)));
//                    double diffFactor = computedDifficulty.get(i) + computedDifficulty.get(j);
//                    proximity += (prox / diffFactor) * conflictMatrix[i][j];
//                }
//            }
//        }
//        return proximity / studentMap.size();
//    }

    public double evaluateMovementFitness(IntegerMatrixSolution<ArrayList<Integer>> solution) {
        //movement constraint
        double movementCost = 0.0;
//        System.out.println("\n\nEVALUATING MOVEMENT COST for solution:");
//        System.out.println(solution.getVariables());
        for (Map.Entry<Integer, Student> currStudent : studentMap.entrySet()) {
//            System.out.println("Student="+currStudent.getKey());
            for (int e1 = 0; e1 < currStudent.getValue().examList.size(); e1++) {
                Exam cExam = examVector.get(currStudent.getValue().examList.get(e1).examId);

                for (int e2 = e1; e2 < currStudent.getValue().examList.size(); e2++) {
                    if (e1 == e2) {
                        continue;
                    }
                    Exam nExam = examVector.get(currStudent.getValue().examList.get(e2).examId);
//                    if(cExam.room==null)continue;int rm1 = cExam.room.roomId;
//                    if(nExam.room==null)continue;int rm2 = nExam.room.roomId;
//                    
//                    fitness2+=roomToRoomDistanceMatrix[rm1-1][rm2-1];
                    int room1 = getRoom(solution.getVariable(cExam.examId));
                    int room2 = getRoom(solution.getVariable(nExam.examId));
                    if (room1 == -1 || room2 == -1) {
                        return Double.MAX_VALUE;
                    }
                    if (room1 > -1 && room2 > -1) {
//                        System.out.println("room="+room1+"\nroom2="+room2);
                        double currCost = roomToRoomDistanceMatrix[room1][room2];
//                        System.out.println("Movement cost btw exam "+cExam.examId+" in room "+(room1+1)+" to exam "+nExam.examId
//                            +" in room "+(room2+1)+" is "+currCost);
                        movementCost += currCost;

                    }
                }
            }
        }
        return movementCost / studentMap.size();
    }    
    
    public double evaluateGSPMovementFitness(IntegerMatrixSolution<ArrayList<Integer>> solution){
        double gspMovementCost = 0.0;
//        System.out.println("solution.getNumberOfVariables() = "+solution.getNumberOfVariables());
//        System.out.println("solution.getLength() = "+solution.getLength());
//        System.out.println("\nEvaluating :"+solution.getVariables()+"\n");
//        for(int exam=0;exam<solution.getLength();exam++){
//            for(int student=1;student<=solution.getVariable(exam).size();student++){
//                int department = studentMap.get(student).deptId;
//                int room = solution.getVariable(exam).get(student-1);
//                if(room==-1)continue;
//                gspMovementCost+=departmentToRoomDistanceMatrix[department][room];
//            }
//        }                
        
        //Compact solution fitness evaluation
        /////////////////////////////////////////////////////////////////////////
        for(int stdIndex=0;stdIndex<solution.getVariable(1).size();stdIndex++){
            int student = studentMap.get(solution.getVariable(1).get(stdIndex)).sId;
            int department = studentMap.get(student).deptId;
            int room = solution.getVariable(2).get(stdIndex);
            if(room==-1){
                System.out.println("\nStudent "+student+"has no room!!!!\n");
                continue;
            }
            gspMovementCost+=departmentToRoomDistanceMatrix[department][room];
        }
        /////////////////////////////////////////////////////////////////////////
        
//        for(Map.Entry<Integer, Student> currStudent: studentMap.entrySet()){
//            int studentId  = currStudent.getKey()-1;
//            int department = currStudent.getValue().deptId; 
////            for(int e=0;e<currStudent.getValue().examList.size();e++){
//            for(Exam e:currStudent.getValue().examList){
//                int room = solution.getVariable(e.examId).get(studentId);
////                int room = e.studentRoomMap.get(currStudent.getKey());
////                int room = currStudent.getValue().examList.get(e).room.roomId;   
////                System.out.println("departmentToRoomDistanceMatrix[department="+department+"][room="+room+"]");
//                gspMovementCost+=departmentToRoomDistanceMatrix[department][room];                
//            }
//        }
        System.out.println("gspMovementCost = "+gspMovementCost);
        return gspMovementCost/studentMap.size();
    }
    
    public int evaluateGspRoomUtilizationFitness(IntegerMatrixSolution<ArrayList<Integer>> solution){
        int gspRoomUtilizationCost = 0;
        
        ArrayList<Integer> usedRooms = new ArrayList();
        for(int i=0;i<solution.getNumberOfVariables();i++){
            int room;
            for(int j=0;j<solution.getVariable(i).size();j++){
                room = solution.getVariable(i).get(j); 
                if(room!=-1){
                    if(!usedRooms.contains(room)){
                        usedRooms.add(room);
                        gspRoomUtilizationCost++;
                    }                    
                }
            }
        }
        return gspRoomUtilizationCost;
    }

    public double evaluateRoomUtilizationFitness(IntegerMatrixSolution<ArrayList<Integer>> solution) {
        //room under-utilization and over-utilization constraint 
        int roomCap = 0, studSize = 0;
        double underUtilization = 0.0;
        double overUtilization = 0.0;

        for (int i = 0; i < solution.getNumberOfVariables(); i++) {
            ArrayList<Integer> exam = solution.getVariable(i);
            int room = getRoom(exam);
            if (room == -1) {
                return Double.MAX_VALUE;
            }
            roomCap = roomVector.get(room).capacity;
            studSize = examVector.get(i).studentsCount;
            if (studSize < roomCap) {
                underUtilization += (roomCap - studSize);
            } else if (studSize > roomCap) {
                overUtilization += (studSize - roomCap);
            }
//            else{               
//                overUtilization+=studSize;
//            }
        }
        return (underUtilization + overUtilization) / roomVector.size();
    }

    public int evaluateITC2007Fitness(IntegerMatrixSolution<ArrayList<Integer>> solution) {
        //TwoInARow,TwoInADay,PeriodSpread
        Timeslot slot1, slot2;
        int twoInARowCount = 0;
        int twoInADayCount = 0;
        int periodSpreadCount = 0;
        for (Student currStudent : studentMap.values()) //        for(int i=0; i < studentMap.size();i++)
        {
            ArrayList<Exam> examList = currStudent.examList;
            for (int j = 0; j < examList.size(); j++) {
                for (int k = 0; k < examList.size(); k++) {
                    if (j == k) {
                        continue;
                    }
//                    System.out.print("-->"+getTimeslot(solution.getVariable(currStudent.getValue().examList.get(j).examId - 1))+" ");
                    int timeS1 = getTimeslot(solution.getVariable(examList.get(j).examId));
                    int timeS2 = getTimeslot(solution.getVariable(examList.get(k).examId));
                    if (timeS1 == -1 || timeS2 == -1) {
                        return Integer.MAX_VALUE;
                    }
                    slot1 = timeslotVector.get(timeS1);
                    slot2 = timeslotVector.get(timeS2);

//                    if (slot1.day == slot2.day) {
                    if (slot1.dateAndTime.getMonth() == slot2.dateAndTime.getMonth()
                            && slot1.dateAndTime.getDay() == slot2.dateAndTime.getDay()) {
                        if (Math.abs(slot1.id - slot2.id) == 1) {
                            twoInARowCount++;
                        } else if (Math.abs(slot1.id - slot2.id) > 1) {
                            twoInADayCount++;
                        }
                    }
                    if (Math.abs(slot1.id - slot2.id) < spreadGap) {
                        periodSpreadCount++;
                    }
                }
            }
        }
        int twoInARowPenalty = twoInARowWeight * twoInARowCount;
        int twoInADayPenalty = twoInADayWeight * twoInADayCount;
        int periodSpreadPenalty = periodSpreadWeight * periodSpreadCount;

        //NonMixedDuration, Room Penalty & Timeslot Penalty
        Map<Integer, Set> roomDurationMap = new HashMap<>();
        int nonMixedDurationPenalty = 0;
        int timeslotPenalty = 0;
        int roomPenalty = 0;
        for (int i = 0; i < timeslotVector.size(); i++) {
            for (int j = 0; j < solution.getNumberOfVariables(); j++) {
                //if(getTimeslot(solution.getVariable(j))==timeslotVector.get(i).id-1)
                if (solution.getVariable(j).get(i) != 0) {
                    int room = getRoom(solution.getVariable(j));
                    if (room == -1) {
                        nonMixedDurationPenalty = Integer.MAX_VALUE;
                        timeslotPenalty = Integer.MAX_VALUE;
                        roomPenalty = Integer.MAX_VALUE;
                        break;
                    }
//                    System.out.println("room="+room);
                    int duration = examVector.get(j).examDuration;

                    if (roomDurationMap.containsKey(room)) {
                        roomDurationMap.get(room).add(duration);
                    } else {
                        roomDurationMap.put(room, new HashSet(duration));
                    }

                    //Timeslot Penalty
                    if (timeslotVector.get(i).penalty != 0) {
                        timeslotPenalty += timeslotVector.get(i).penalty;
                    }

                    //Room Penalty
                    if (room >= 0) {
                        if (roomVector.get(room).penalty != 0) {
                            roomPenalty += roomVector.get(room).penalty;
                        }
                    }
                }
            }

            ArrayList<Set> durations = new ArrayList<>();
            durations.addAll(roomDurationMap.values());
            for (int j = 0; j < durations.size(); j++) {
                nonMixedDurationPenalty += (durations.get(j).size() - 1) * nonMixedDurationsWeight;
            }
        }

        //Frontload
        int frontLoadViolation = 0;

        for (int i = 0; i < examVector.size(); i++) {
            if (examVector.get(i).studentsCount >= numberOfLargestExams) {
                largestExams.add(examVector.get(i).examId);
            }
        }

        for (int i = 0; i < solution.getNumberOfVariables(); i++) {
            int timeslot = getTimeslot(solution.getVariable(i));
            if (timeslot == -1) {
                frontLoadViolation = Integer.MAX_VALUE;
                break;
            }
            if (largestExams.contains(i) && timeslot > (timeslotVector.size() - numberOfLastPeriods)) {
                frontLoadViolation++;
            }
        }
        int frontLoadPenalty = frontLoadViolation * frontLoadWeight;

//        System.out.println("\n\ntwoInARowPenalty = " + twoInARowPenalty);
//        System.out.println("twoInADayPenalty = " + twoInADayPenalty);
//        System.out.println("periodSpreadPenalty = " + periodSpreadPenalty);
//        System.out.println("nonMixedDurationPenalty = " + nonMixedDurationPenalty);
//        System.out.println("frontLoadPenalty = " + frontLoadPenalty);
//        System.out.println("timeslotPenalty = " + timeslotPenalty);
//        System.out.println("roomPenalty = " + roomPenalty);
//        System.out.println("\n");
        //ITC2007 Objective Function
        return twoInARowPenalty + twoInADayPenalty + periodSpreadPenalty
                + nonMixedDurationPenalty + frontLoadPenalty + timeslotPenalty + roomPenalty;
    }

    public void evaluateConstraints(IntegerMatrixSolution<ArrayList<Integer>> solution) {
        //conflicts
//        System.out.println("EVALUATING CONFLICTS");
        int conflicts = 0;
        for (int i = 0; i < solution.getNumberOfVariables(); i++) {
            for (int j = 0; j < solution.getNumberOfVariables(); j++) {
                if (i == j) {
                    continue;
                }

//                int slot1 = 0, slot2 = 0;
                int gspSlot1 = examVector.get(i).timeslot.id;
                int gspSlot2 = examVector.get(j).timeslot.id;
//                System.out.println("\tExam "+i+" @ timeslot "+gspSlot1);
//                System.out.println("\tExam "+i+" @ timeslot "+gspSlot1);
//                ArrayList<Integer> x = solution.getVariable(i);
//                slot1 = getTimeslot(x);
                
//                while (x.get(slot1) == 0) {
//                    slot1++;
//                }

//                ArrayList<Integer> y = solution.getVariable(j);
//                slot2 = getTimeslot(y);
                
//                while (y.get(slot2) == 0) {
//                    slot2++;
//                }

//                if (slot1 == -1 || slot2 == -1) {
                if (gspSlot1 == -1 || gspSlot2 == -1) {
                    conflicts = Integer.MAX_VALUE;
                    break;
                }
                if (conflictMatrix[i][j] != 0) {
//                    if (slot1 == slot2) {
                    if (gspSlot1 == gspSlot2) {                        
                        conflicts++;
//                        System.out.println("Exam "+i+" conflicts with "+j);
                    }
                }
            }
        }//System.out.println("conflicts="+conflicts);

        //room under-utilization constraint
//        int roomOccupancyViolation = 0;
//        int examCapacity = 0;
//        int roomCapacity = 0;
//
//        for (int i = 0; i < solution.getNumberOfVariables(); i++) {
//            ArrayList<Integer> exam = solution.getVariable(i);
//
//            for (int j = 0; j < exam.size(); j++) {
//                if (exam.get(j) == 0) {
//                    continue;
//                }
//                int room = exam.get(j);
//
//                if (room != -1) {
//                    examCapacity = examVector.get(i).studentsCount;
//                    roomCapacity = roomVector.get(room - 1).capacity;
//
//                    //roomCapacity=examVector.get(i).room.capacity;
//                    if (examCapacity > roomCapacity) {
//                        roomOccupancyViolation++;
//                    }
//                }
//            }
//        }//System.out.println("roomOccupancyViolation="+roomOccupancyViolation);

        //Timeslot Utilisation
        int timeslotUtilisationViolation = 0;
        int timeslotDuration = 0;
        int examDuration = 0;
        for (int i = 0; i < solution.getNumberOfVariables(); i++) {
//            ArrayList<Integer> exam = solution.getVariable(i);

            timeslotDuration = examVector.get(i).timeslot.duration;
            examDuration = examVector.get(i).examDuration;
            if (timeslotDuration < examDuration) {
                timeslotUtilisationViolation++;
            }
            
//            for (int j = 0; j < exam.size(); j++) {
//                if (exam.get(j) == 0) {
//                    continue;
//                }
//                timeslotDuration = timeslotVector.get(j).duration;
//                examDuration = examVector.get(i).examDuration;
//                if (timeslotDuration < examDuration) {
//                    timeslotUtilisationViolation++;
//                }
//            }
        }//System.out.println("timeslotUtilisationViolation="+timeslotUtilisationViolation);

        //PeriodOrdering
//        int periodOrderingViolation = 0;
//        for (int i = 0; i < solution.getNumberOfVariables(); i++) {
//            ArrayList<Integer> x = solution.getVariable(i);
//            int k = getTimeslot(x);
//            for (int j = 0; j < solution.getNumberOfVariables(); j++) {
//                ArrayList<Integer> y = solution.getVariable(i);
//                int l = getTimeslot(y);
////                for (k = 0; k < solution.getVariable(i).size(); k++) {
////                    if (solution.getVariable(i).get(k) != 0) {
////                        break;
////                    }
////                }
////                for (l = 0; l < solution.getVariable(i).size(); l++) {
////                    if (solution.getVariable(j)
////                            .get(l) != 0) {
////                        break;
////                    }
////                }
//
//                if (exclusionMatrix[i][j] != 0) {
//                    if (k == l) {
//                        periodOrderingViolation++;
//                    }
//                }
//                if (afterMatrix[i][j] != 0) {
//                    if (k < l) {
//                        periodOrderingViolation++;
//                    }
//                }
//                if (coincidenceMatrix[i][j] != 0) {
//                    if (conflictMatrix[i][j] != 0) {
//                        break;
//                    }
//                    if (k != l) {
//                        periodOrderingViolation++;
//                    }
//                }
//            }
//        }//System.out.println("periodOrderingViolation="+periodOrderingViolation);

        //RoomConstraints
//        int roomConstraintViolation = 0;
//        for (int i = 0; i < exclusiveExamsVector.size(); i++) {
//            int exclusiveExam = exclusiveExamsVector.get(i).examId;
//            int k, exclusiveRoom = -1;
//            for (k = 0; k < solution.getVariable(exclusiveExam).size(); k++) {
//                if (solution.getVariable(exclusiveExam).get(k) != 0) {
//                    exclusiveRoom = solution.getVariable(exclusiveExam).get(k);
//                    break;
//                }
//            }
//
//            for (int j = 0; j < solution.getNumberOfVariables(); j++) {
//                if (j == exclusiveExam) {
//                    continue;
//                }
//                int r, room = -1;
//                for (r = 0; r < solution.getVariable(j).size(); r++) {
//                    if (solution.getVariable(j).get(r) != 0) {
//                        room = solution.getVariable(j).get(r);
//                        break;
//                    }
//                }
//                if (exclusiveRoom == room) {
//                    roomConstraintViolation++;
//                }
//            }
//        }//System.out.println("roomConstraintViolation="+roomConstraintViolation);

        solution.setAttribute("CONFLICTS", conflicts);
        solution.setAttribute("TIMESLOT_PENALTY", timeslotUtilisationViolation);
//        solution.setAttribute("ROOM_UTILIZATION_PENALTY", roomOccupancyViolation);        
//        solution.setAttribute("TIMESLOT_ORDERING_PENALTY", periodOrderingViolation);
//        solution.setAttribute("ROOM_CONSTRAINT_PENALTY", roomConstraintViolation);
    }

    @Override
    public int getLength() {
        return numberOfExams;
    }

    @Override
    public ArrayList<Integer> getListOfExamsPerVariable() {
        ArrayList<Integer> list = new ArrayList<>(Arrays.asList(numberOfExams));
        return list;
    }

    public int getTimeslot(ArrayList<Integer> exam) {

        for (int i = 0; i < exam.size(); i++) {
            if (exam.get(i) != -1) {
                return i;
            }
        }
//        System.out.println("Timeslot not found for exam "+exam.toString());
        return -1;
    }

    public int getRoom(ArrayList<Integer> exam) {
        for (int i = 0; i < exam.size(); i++) {
            if (exam.get(i) != -1) {
                return exam.get(i);
            }
        }
//        System.out.println("Room not found for exam "+exam.toString());
        return -1;
    }

    @Override
    public int[] getRoomCapacities() {
        int[] roomCapacities = new int[roomVector.size()];
        for (int i = 0; i < roomVector.size(); i++) {
            roomCapacities[i] = roomVector.get(i).capacity;
        }
        return roomCapacities;
    }

    @Override
    public int[] getExamEnrollments() {
        int[] examEnrollments = new int[examVector.size()];
        for (int i = 0; i < examVector.size(); i++) {
            examEnrollments[i] = examVector.get(i).studentsCount;
        }
        return examEnrollments;
    }

    @Override
    public int getNumberOfTimeslots() {
        return numberOfTimeslots;
    }

    public ArrayList getLargestExams() {
        return largestExams;
    }
}
