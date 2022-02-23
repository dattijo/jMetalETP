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

import net.sourceforge.jFuzzyLogic.FIS;
import net.sourceforge.jFuzzyLogic.FunctionBlock;
import net.sourceforge.jFuzzyLogic.plot.JFuzzyChart;
import net.sourceforge.jFuzzyLogic.rule.Variable;

/**
 *
 * @author aadatti
 */
public final class MultiobjectiveETP extends AbstractIntegerMatrixProblem {

    FIS fis;
    NumberFormat formatter = new DecimalFormat("#0000.0000");

    Map<Integer, Student> studentMap;
    ArrayList<Exam> examVector;
    ArrayList<Exam> exclusiveExamsVector;
    ArrayList<Timeslot> timeslotVector;
    ArrayList<Room> roomVector;
    ArrayList<Department> departmentVector;
    ArrayList<ArrayList<Integer>> timetableSolution;
    ArrayList<Integer> largestExams;
    ArrayList<Integer> courseType;
    ArrayList<Integer> courseCredits;
    ArrayList<Double> successRatio;
    ArrayList<Integer> perceivedDifficulty;
    ArrayList<Double> computedDifficulty;

    int numberOfExams;
    int numberOfTimeslots;
    int numberOfCampuses;
    int numberOfDepartments;
    int numberOfFaculties;
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

    int[][] exclusionMatrix;
    int[][] coincidenceMatrix;
    int[][] afterMatrix;

    boolean feasible;

    Graph<Integer, DefaultEdge> exGraph;
    Coloring coloredGraph;

    class Student {

        int sId, roomId=-1, deptId;
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
        
        void setRoom(int r){
            roomId = r;
        }
        
    }

    class Exam {

        int examId, examDuration, priority, studentsCount = 0, dSizeToHCRatio, allocationCount, deAllocationCount, difficulty;
        double fuzzyDifficulty;
        boolean exlcusive = false;
        Timeslot timeslot;
        Room room;         
        ArrayList<Student> enrollmentList;
        PriorityQueue<TimeslotRoom> timeslotRoomQueue;
        
        Exam(int id, int duration) {
            this.enrollmentList = new ArrayList<>();
            this.timeslotRoomQueue = new PriorityQueue<>(new TimeslotRoomComparator());
            examId = id;
            examDuration = duration;
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

        void setDifficulty(int d) {
            difficulty = d;
        }
        
        
        void setFuzzyDifficulty(double d) {
            fuzzyDifficulty = d;
        }
        
        void setDSizeToHCRatio(){
            dSizeToHCRatio = timeslotRoomQueue.size();
        }

        int getDifficulty() {
            return difficulty;
        }
        
        TimeslotRoom getTimeslotRoom(TimeslotRoom tr){            
            for(TimeslotRoom trQElement: timeslotRoomQueue){
                if(tr.timeslot==trQElement.timeslot&&tr.room==trQElement.room){
                    return trQElement;
                }
            }
            return null;
        }
        
        TimeslotRoom getTimeslotRoom(int t, int r){            
            for(TimeslotRoom trQElement: this.timeslotRoomQueue){
                if(t==trQElement.timeslot&&r==trQElement.room){
                    return trQElement;
                }
            }
            return null;
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
        PriorityQueue<Room> roomsQueue = new PriorityQueue<>(new RoomComparatorByDistance());
        
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
        double averageDifficulty=0.0;
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
        
        void computeAverageDifficulty(){
            double sum=0.0;
            for(int i=0;i<examList.size();i++){
                sum+=examList.get(i).getDifficulty();
            }
            averageDifficulty=sum/examList.size();
        }
    }

    class Conflict {

        Exam conflictingExam;
        int evictionCount;

        Conflict(Exam e, int eC) {
            conflictingExam = e;
            evictionCount = eC;
        }
    }

    class TimeslotRoom {

        ArrayList<Conflict> conflicts;

        int timeslot, room, possibleConflicts;
        long rank;

        //Default Constructor
        TimeslotRoom() {
            timeslot = -1;
            room = -1;
            rank = Long.MAX_VALUE;
            conflicts = new ArrayList();                     
        }
        
        //Parametrized Constructor
        TimeslotRoom(int t1, int r1) {            
            timeslot = t1;
            room = r1;
            rank = 0;            
            conflicts = new ArrayList();
        }
        
        //Copy Constructor
        TimeslotRoom(TimeslotRoom trP) {            
            this.timeslot = trP.timeslot;
            this.room = trP.room;
            this.rank = trP.rank;            
            this.conflicts = new ArrayList<>(trP.conflicts.size());            
            for(Conflict c:trP.conflicts){                
                this.conflicts.add(new Conflict(c.conflictingExam,c.evictionCount));                
            }
        }

//        void computeRank() {
//            int sum = 0;
//            for (Conflict con : conflicts) {
//                sum += con.evictionCount;
//            }
//            rank = sum;
//        }
    }

    class ExamComparatorByEnrollment implements Comparator<Exam> {

        @Override
        public int compare(Exam a, Exam b) {
            return a.studentsCount < b.studentsCount ? -1 : a.studentsCount == b.studentsCount ? 0 : 1;
        }
    }

    class ExamComparatorByPriority implements Comparator<Exam> {

        @Override
        public int compare(Exam a, Exam b) {
            return a.priority < b.priority ? -1 : a.priority == b.priority ? 0 : 1;
        }
    }
    
    class ExamComparatorByRatio implements Comparator<Exam> {

        @Override
        public int compare(Exam a, Exam b) {
            return a.dSizeToHCRatio < b.dSizeToHCRatio ? -1 : a.dSizeToHCRatio == b.dSizeToHCRatio ? 0 : 1;
        }
    }

    class RoomComparator implements Comparator<Room> {

        @Override
        public int compare(Room a, Room b) {
            return a.capacity < b.capacity ? -1 : a.capacity == b.capacity ? 0 : 1;
        }
    }

    class RoomComparatorByDistance implements Comparator<Room> {

        @Override
        public int compare(Room a, Room b) {            
            return a.distanceToDept < b.distanceToDept ? -1 : a.distanceToDept == b.distanceToDept ? 0 : 1;
        }
    }
    
    class TimeslotRoomComparator implements Comparator<TimeslotRoom> {

        @Override
        public int compare(TimeslotRoom a, TimeslotRoom b) {
            return a.rank < b.rank ? -1 : a.rank == b.rank ? 0 : 1;
        }
    }
    
    class TimeslotComparatorByDifficulty implements Comparator <Timeslot>{

        @Override
        public int compare(Timeslot a, Timeslot b) {
//            return a.averageDifficulty < b.averageDifficulty? -1: a.averageDifficulty==b.averageDifficulty?0:1;
            Double d1 = a.averageDifficulty;
            Double d2 = b.averageDifficulty;
            return d1.compareTo(d2);
        }
    
    }

    public MultiobjectiveETP(String problemFile, String fuzzySystem, String examDifficultyData) throws IOException {
        studentMap = new HashMap<>();
        examVector = new ArrayList<>();
        exclusiveExamsVector = new ArrayList<>();
        timeslotVector = new ArrayList();
        roomVector = new ArrayList();
        departmentVector = new ArrayList();
        timetableSolution = new ArrayList<>();
        largestExams = new ArrayList<>();
        spreadGap = 0;

        conflictMatrix = readProblem(problemFile);

        roomToRoomDistanceMatrix = new double[numberOfRooms][numberOfRooms];

        generateDistanceMatrices();

        exGraph = new SimpleGraph<>(DefaultEdge.class);
        createGraph(conflictMatrix);
        generateDifficultyMatrix(fuzzySystem, examDifficultyData);
        setNumberOfVariables(numberOfExams);
        setNumberOfObjectives(1);
//        this.setNumberOfConstraints(5);
        setName("ETP");
    }

    @Override
    public int[][] getConflictMatrix() {
        return conflictMatrix;
    }

    private boolean generateDifficultyMatrix(String fuzzySystem, String examDifficultyData) throws IOException {
        courseType = new ArrayList();
        courseCredits = new ArrayList();
        successRatio = new ArrayList();
        perceivedDifficulty = new ArrayList();
        computedDifficulty = new ArrayList();

        readExamDifficultyData(examDifficultyData);

        fis = FIS.load(fuzzySystem, true);
        if (fis == null) {
            System.err.println("Can't load file: '" + fuzzySystem + "'");
            return false;
        }
        for (int i = 0; i < numberOfExams; i++) {
            FunctionBlock examDifficulty = fis.getFunctionBlock("examDifficulty");

            fis.setVariable("credits", courseCredits.get(i));
            fis.setVariable("type", courseType.get(i));
            fis.setVariable("sratio", successRatio.get(i));
            fis.setVariable("pdifficulty", perceivedDifficulty.get(i));
//            JFuzzyChart.get().chart(examDifficulty);
            fis.evaluate();

            Variable difficulty = examDifficulty.getVariable("difficulty");
//            Variable var = examDifficulty.getVariable("type");
//            JFuzzyChart.get().chart(var, true);
//            JFuzzyChart.get().chart(difficulty, difficulty.getDefuzzifier(), true);    
            double d = difficulty.getValue();
            computedDifficulty.add(d);
            examVector.get(i).setFuzzyDifficulty(d);
        }
//        System.out.println("Sums\t\t\tProducts");
//        for(int i=0; i<numberOfExams-1;i++){
//            for(int j=0; j<numberOfExams;j++){
//                if(i==j)continue;
//                System.out.print(computedDifficulty.get(i)+computedDifficulty.get(j)+"\t");
//                System.out.println(computedDifficulty.get(i)*computedDifficulty.get(j));    
//            }
//        }
        return true;
    }

    private boolean readExamDifficultyData(String file) throws IOException {
        InputStream in = getClass().getResourceAsStream(file);
        if (in == null) {
            in = new FileInputStream(file);
        }

        InputStreamReader isr = new InputStreamReader(in);
        BufferedReader br = new BufferedReader(isr);

        StreamTokenizer token = new StreamTokenizer(br);

        token.eolIsSignificant(true);
        boolean found = false;

        int t = 0;
        while (!found && t != StreamTokenizer.TT_EOF) {
            switch (t) {
                case StreamTokenizer.TT_NUMBER:
//                    System.out.println("1token.nval "+token.nval);
                    courseType.add((int) token.nval);
                    token.nextToken();
                    token.nextToken();
//                    System.out.println("2token.nval "+token.nval);
                    courseCredits.add((int) token.nval);
                    token.nextToken();
                    token.nextToken();
//                    System.out.println("3token.nval "+token.nval);
                    successRatio.add(token.nval);
                    token.nextToken();
                    token.nextToken();
//                    System.out.println("4token.nval "+token.nval);
                    perceivedDifficulty.add((int) token.nval);
                    break;
                case StreamTokenizer.TT_EOL:
                    token.nextToken();
                    break;
            }
            t = token.nextToken();
            token.nextToken();
        }

//        System.out.println("courseType : "+courseType.toString());
//        System.out.println("courseCredits : "+courseCredits.toString());
//        System.out.println("successRatio : "+successRatio.toString());
//        System.out.println("perceivedDifficulty : "+perceivedDifficulty.toString());  
        return true;
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
        
        setRandomDifficultyIndices();
        setExamPriorities();
        return conflictMatrix;
    }

    int[][] readExams(StreamTokenizer tok, boolean fnd) throws IOException {
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
        System.out.println("No of exams = "+numberOfExams);
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
                System.out.println("Number of Depts = "+numberOfDepartments);
                fnd = true;
            } else {
                t = tok.nextToken();

                switch (t) {
                    case StreamTokenizer.TT_EOL:
                        tok.nextToken();
                        addExam(tok);
                        break;
                    case StreamTokenizer.TT_NUMBER:
                        int currentStudent = (int) tok.nval-1;
                        if (!studentMap.containsKey(currentStudent)) {
                            studentMap.put(currentStudent, new Student(currentStudent));
                            
                        }
                        examVector.get(tok.lineno() - 2).addStudent(studentMap.get(currentStudent));
//                        System.out.println("Student "+currentStudent+" added to exam "+(tok.lineno()-2));
                        studentMap.get(currentStudent).examList.add(examVector.get(tok.lineno() - 2));
                        break;
                }
            }
        }

//        Print Student Map
//        for(Map.Entry<Integer,Student> entry : studentMap.entrySet())            
//        {
////            System.out.print("Student " + entry.getKey()+" Exams: ");
//            for(int i =0;i<entry.getValue().examList.size();i++)
//            {
//                System.out.print(entry.getValue().examList.get(i).examId+",");
//            }
//            System.out.println();
//        }
        conflictMatrix = new int[numberOfExams][numberOfExams];

        //Generate Conflict Matrix
        ArrayList<Student> cleared = new ArrayList();
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
                        int conflictCount = conflictMatrix[currExam][nextExam];
                        conflictCount++;
                        conflictMatrix[currExam][nextExam] = conflictCount;
                        conflictMatrix[nextExam][currExam] = conflictCount;
                    }
                }
            }
        }

//        int Matrix ConflictMatrix;
//        System.out.println("\nDISPLAYING int[][] Matrix CONFLICT MARIX:\n");
//        for(int i=0;i<numberOfExams;i++)
//        {
//            for(int j=0;j<numberOfExams;j++)
//            {
//                System.out.print(conflictMatrix[i][j]+", ");
//            }
//            System.out.println();
//        }
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
//        System.out.println("Reading Enrollment By Department");
        fnd = false;
        int t, student, deptCount = -1;
            
        while (!fnd) {
            if ((tok.sval != null) && ((tok.sval.compareTo("Periods") == 0))) {
                tok.nextToken();
                tok.nextToken();
                numberOfTimeslots = (int) tok.nval;
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
                        student = (int) tok.nval-1;

//                        tok.nextToken();
//                        tok.nextToken();          
                        if(studentMap.containsKey(student)){
                            studentMap.get(student).deptId=deptCount;
//                            System.out.println("Reading Student "+student+" from dept "+deptCount);
                        }
                        else{
//                            System.out.println("\tStudent "+student+" not found");
                        }                        
                        break;
                }
            }
        }

////        System.out.println("Enrollment Count By Department");
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
        System.out.println("Number of Timeslots = "+numberOfTimeslots);
        fnd = false;
        int t, tCount = 0;
        int day, month, year, hour, minutes, seconds, duration, penalty;
        
        day = month = year = hour = minutes = seconds = duration = penalty = 0;
        while (!fnd) {             
            if ((tok.sval != null) && ((tok.sval.compareTo("Departments") == 0))) {
                tok.nextToken();
                tok.nextToken();
//                System.out.println("Finished Reading Timeslots.");
                fnd = true;
            } else {
                t = tok.nextToken();
                switch (t) {                    
                    case StreamTokenizer.TT_EOL:                                  
                        break;
                    case StreamTokenizer.TT_NUMBER:
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
//                        tok.nextToken();                         
                        Date examDateAndTime = new Date(year - 1900, month - 1, day, hour, minutes, seconds);                        
                        addTimeslot(tCount++, examDateAndTime, duration, penalty);
//                        System.out.println("examDateAndTime = "+examDateAndTime.toLocaleString());
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
//        System.out.println("Reading Departments");
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
//            System.out.println(" and latitude "+departmentVector.get(i).latitude);        
//        }
    }

    void readRooms(StreamTokenizer tok, boolean fnd) throws IOException {
        //Read Rooms
        System.out.println("Number of Rooms = "+numberOfRooms);
        fnd = false;
//        int t, rCount = 0, cap, fac, dToFac, penalty;
        int t, rCount = 0, cap, penalty;
        double lon, lat;
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

                        lon = (double) tok.nval;
                        tok.nextToken();
                        tok.nextToken();

                        lat = (double) tok.nval;
                        tok.nextToken();
                        tok.nextToken();

                        penalty = (int) tok.nval;
                        tok.nextToken();

                        addRoom(cap, rCount++, lon, lat, penalty);
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
        fnd = false;
        int t;
        while (!fnd) {
            if ((tok.sval != null) && ((tok.sval.compareTo("InstitutionalWeightings") == 0))) {
                tok.nextToken();
                tok.nextToken();
                fnd = true;
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
    
    void setRandomDifficultyIndices(){        
        for(Exam ex: examVector){
            Integer rand = new Random().nextInt(2);
            ex.setDifficulty(rand.intValue());
//            System.out.println("Exam "+ex.examId+" has "+ex.difficulty+" difficulty level.");
        }
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
                
                double distance = gpsDistance(long1, lat1, long2, lat2);
                
                roomToRoomDistanceMatrix[i][j] = roomToRoomDistanceMatrix[j][i] = distance;                                            
                
            }
        }
        
        //Displa roomToRoomDistanceMatrix
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
        return d * 1000; // meters          
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

    void addDepartment(int id, double lon, double lat){
        departmentVector.add(new Department(id,lon,lat));
    }

    void addRoom(int c, int id, double lon, double lat, int pen) {
        roomVector.add(new Room(c, id, lon, lat, pen));
//        overallRoomCapacity+=c;
    }

//    void addCampus(int id, double lon, double lat) {
//        campusVector.add(new Campus(id, lon, lat));
//    }

    boolean allocateTimeslots() {
//        int roomsCapacity=0;
//        for(Room rm: roomVector){
//            roomsCapacity += rm.capacity;
//        }
//        System.out.println("Total Room Capacity = "+roomsCapacity);
        try {
            int numberOfColors = 0;
            //        ArrayList<Timeslot> availableTimeslots = new ArrayList<>();
//        availableTimeslots.addAll(timeslotVector);
//        System.out.println("availableTimeslots.size()="+availableTimeslots.size());
//        System.out.println("timeslotVector.size()="+timeslotVector.size());
            for (int t = 0; t < timeslotVector.size(); t++) {
//        for (int t = 0; t < availableTimeslots.size(); t++) {
//            availableTimeslots.get(t).examList.clear();
                timeslotVector.get(t).examList.clear();
            }
//          coloredGraph  = new LargestDegreeFirstColoring(exGraph).getColoring();
//        coloredGraph  = new GreedyColoring(exGraph).getColoring();           
//          coloredGraph  = new BrownBacktrackColoring(exGraph).getColoring();
//            coloredGraph = new RandomGreedyColoring(exGraph).getColoring();
            coloredGraph = new SmallestDegreeLastColoring(exGraph).getColoring();
//            coloredGraph = new SaturationDegreeColoring(exGraph).getColoring();

            numberOfColors = coloredGraph.getNumberColors();
            System.out.println("Number of Timeslots = " + numberOfTimeslots);
            System.out.println("Number of Colors = " + numberOfColors);
            if (numberOfColors <= numberOfTimeslots) {
                System.out.println("Solution exist");
            } else {
                System.out.println("Solution doesn't exist");
                return false;
            }

            for (Exam exam : examVector) {
                //int allocatedTime = ;
                int allocatedTime = (int) coloredGraph.getColors().get(exam.examId);
//            Exam exam = examVector.get(i);
//            System.out.println("allocatedTime="+allocatedTime);
//            e.setTimeslot(availableTimeslots.get(allocatedTime));
//            availableTimeslots.get(allocatedTime).addExam(e);
//            exam.setTimeslot(allocatedTime);   
                examVector.get(exam.examId).setTimeslot(allocatedTime);
                timeslotVector.get(allocatedTime).addExam(exam.examId);
                System.out.println("Exam "+examVector.get(exam.examId).examId+" @ Timeslot "+examVector.get(exam.examId).timeslot.id);                        
            }

//            for(int t=0;t<timeslotVector.size();t++)
//            {
//                int totStudents=0;
//                for(Exam ex: timeslotVector.get(t).examList){
//                    totStudents+=ex.studentsCount;
//                }
//                
//                System.out.println("Timeslot "+timeslotVector.get(t).id+" has "+timeslotVector.get(t).examList.size()+" exams and "
//                        +totStudents+" total students");
//                
//            }
//        for(int t=0;t<availableTimeslots.size();t++)
//        {
//            System.out.println("Timeslot "+availableTimeslots.get(t).id+" has "+availableTimeslots.get(t).examList.size()+" exams");
//        }
            return true;
        } catch (Exception e) {
            System.out.println("Cannot Allocate Timeslots");
            return false;
        }
    }
    
    boolean allocateTimeslotsByDifficulty(){ 
//        System.out.println("Allocating Timeslots By Difficulty");
        PriorityQueue<Timeslot> usedTimeslotsQ = new PriorityQueue<>(new TimeslotComparatorByDifficulty());
        PriorityQueue<Timeslot> hardTimeslots = new PriorityQueue<>(new TimeslotComparatorByDifficulty());
        PriorityQueue<Timeslot> easyTimeslots = new PriorityQueue<>(new TimeslotComparatorByDifficulty());
        
        ArrayList<ArrayList<Integer>> examGroups = new ArrayList<>();       
        ArrayList<Timeslot> newTimeslotList = new ArrayList<>();                                  
        
        for (int t = 0; t < numberOfTimeslots; t++) {       
            timeslotVector.get(t).examList.clear();
            examGroups.add(timeslotVector.get(t).id, new ArrayList<Integer>());
        }
        
        
        coloredGraph = new SaturationDegreeColoring(exGraph).getColoring();
        int allocatedTime;
        for (Exam exam : examVector) {            
            allocatedTime = (int) coloredGraph.getColors().get(exam.examId);
            timeslotVector.get(allocatedTime).addExam(exam.examId);
            examGroups.get(allocatedTime).add(exam.examId);
        }        
        
        for(Timeslot currentTimeslot:timeslotVector){
            if(!currentTimeslot.examList.isEmpty()){
                currentTimeslot.computeAverageDifficulty();
            }            
        } 
        
//        System.out.println("Initial Graph Heuristic Allocation:");
        for(Timeslot tS:timeslotVector){
//            System.out.println("\tTimeslot "+tS.id+". Number of Exams = "+tS.examList.size());
            if(!tS.examList.isEmpty()){
                usedTimeslotsQ.add(tS); 
            }                                        
        }

//        for (int t = 0; t < numberOfTimeslots; t++) {       
//            System.out.println("\tTimeslot "+t+". Number of Exams = "+examGroups.get(t).size());
////            for(Exam ex:examGroups.get(t)){
////                System.out.print(ex.examId+", ");
////            }
////            System.out.println();
//        }
        
        int usedSlotsCount = usedTimeslotsQ.size();
        
        int mid = Math.floorDiv(usedTimeslotsQ.size(),2);
//        System.out.println(usedTimeslotsQ.size()+" Timeslots split @ "+mid);
//        System.out.println("timeslotsQueue: ");
        ArrayList<Timeslot> tmpQ = new ArrayList<>();
        tmpQ.addAll(usedTimeslotsQ);
//        for(Timeslot tS:tmpQ){
//            System.out.println("\t"+tS.id+". AvgD = "+tS.averageDifficulty);
//        }
        
//        System.out.println("\n\nHardtimeslots: ");
        for(int i=0;i<mid;i++){
            Timeslot timeS = usedTimeslotsQ.poll();
//            System.out.print(" "+timeS.id);
            hardTimeslots.add(timeS);            
        }
//        System.out.println("\n Hard.size()"+hardTimeslots.size());
//        System.out.println("timeslotsQueue.size() now = "+usedTimeslotsQ.size());
        
//        System.out.println("\n\nEasytimeslots: ");
        
        int size = usedTimeslotsQ.size();
        for(int i=0;i<size;i++){            
            Timeslot timeS = usedTimeslotsQ.poll();
//            System.out.println(" "+timeS.id);
            easyTimeslots.add(timeS);            
            
        }
//        System.out.println("\nEasy.size()"+easyTimeslots.size());
                                
        int flag=1;
        for(int i=0; i<usedSlotsCount;i++){
            if(flag>0) newTimeslotList.add(hardTimeslots.poll());                
            else newTimeslotList.add(easyTimeslots.poll());
                        
            if(hardTimeslots.isEmpty()) flag=-1;                           
            else flag*=-1;                 
        }       
        

        
//        System.out.println("\n\n"+newTimeslotList.size()+" New Timeslot Arranged by interlaced difficulty:");
        
//        for(int i=0; i<newTimeslotList.size();i++){            
//            System.out.print(newTimeslotList.get(i).id+" ");
//        }
//        timeslotVector.clear();
        
        for (int t = 0; t < newTimeslotList.size(); t++){            
            timeslotVector.get(t).examList.clear();
            for(Integer ex:examGroups.get(newTimeslotList.get(t).id)){
                timeslotVector.get(t).addExam(ex);
                examVector.get(ex).setTimeslot(t);
            }
//            
//            if(!newTimeslotList.contains(timeslotVector.get(t))){
//                timeslotVector.get(t).averageDifficulty=0.0;
//                System.out.println("Timeslot "+timeslotVector.get(t).id+" unused. AvgD = "+timeslotVector.get(t).averageDifficulty);
//                continue;
//            }
            
//            Timeslot currentTimeslot = newTimeslotList.get(t);
//            currentTimeslot.examList.clear();
//            
//            timeslotVector.add(currentTimeslot);
        }       
//        System.out.println("\nClearing unused slots...");
        for(int i=newTimeslotList.size();i<numberOfTimeslots;i++){
            timeslotVector.get(i).examList.clear();
            timeslotVector.get(i).averageDifficulty=0.0;
        }
//        System.out.println("Done.");
        
//        System.out.println("\n\nFinal Allocation");
//        for(Timeslot timeS: timeslotVector){ 
//            System.out.println("Timeslot "+timeS.id+" has "+timeS.examList.size()+" exams. (Average Difficulty: "+timeS.averageDifficulty+")");
//        }
        return true;
    }

    boolean allocateRooms(ArrayList rooms) {
        try {
            //        System.out.println("\n\nROOM ALLOCATION FOR NEW SOLUTION:");
            Map<Integer, ArrayList> freeTimeslotRoomMap = new HashMap();
            ArrayList<Exam> unAllocatedExams = new ArrayList();
            ArrayList<Exam> mainUnAllocatedExams = new ArrayList();
            ArrayList<Room> tmpRoomVector = new ArrayList();
            int overallCapacity = 0;
            for (Room rm : roomVector) {
                overallCapacity += rm.capacity;
            }
            tmpRoomVector.addAll(roomVector);
            Collections.sort(tmpRoomVector, new RoomComparator().reversed());
//        for(int r =0; r<tmpRoomVector.size();r++){
//            System.out.println("Room "+tmpRoomVector.get(r).roomId+". Capacity = "+tmpRoomVector.get(r).capacity);
//        } 
            for (Timeslot tmpT : timeslotVector) {
//        for (int t = 0; t < timeslotVector.size(); t++) {
//            if (timeslotVector.get(t).examList.size() <= 0) {
//                continue;
//            }            
                int enrollment = 0;
                for (Exam e : tmpT.examList) {
                    enrollment += e.studentsCount;
                }

//            System.out.println("Total Room Capacity = "+overallCapacity);
//            System.out.println("Total students assigned to timeslot "+tmpT.id+" = "+enrollment);
                tmpRoomVector.clear();
                tmpRoomVector.addAll(roomVector);

                Collections.sort(tmpRoomVector, new RoomComparator().reversed());

//            Timeslot tmpT = timeslotVector.get(t);
//            System.out.println("\nNow in Timeslot "+tmpT.id+" having "+tmpT.examList.size()+" exams.");
                ArrayList<Exam> tmpExamVector = new ArrayList();
                tmpExamVector.addAll(tmpT.examList);
                Collections.sort(tmpExamVector, new ExamComparatorByEnrollment().reversed());
//            for(int e =0; e<tmpExamVector.size();e++)
//            {
//                System.out.println("Exam "+tmpExamVector.get(e).examId+". Enrollment = "+tmpExamVector.get(e).studentsCount);                
//            } 

                int e = 0;
                while (e < tmpExamVector.size()) {
                    //System.out.println("Allocating rooms to "+tmpExamVector.size()+" exams...");
                    int r = 0;

                    //System.out.println("Now in Exam "+tmpExamVector.get(e).examId);
                    while (tmpRoomVector.size() > 0 && r < tmpRoomVector.size() && (tmpExamVector.size() > 0) && e < tmpExamVector.size()) {
                        //System.out.println("\nSearching for room to exam "+tmpExamVector.get(0).examId);
                        Exam tmpE = tmpExamVector.get(e);
                        Room tmpR = tmpRoomVector.get(r);
                        if (tmpE.studentsCount <= tmpR.capacity) {
//                        tmpE.setRoom(tmpR);
                            //System.out.println("Exam "+tmpE.examId+" has been set to room "+tmpR.roomId);
//                        allocatedExams.add(tmpE);
                            examVector.get(tmpE.examId).setRoom(tmpR.roomId);//System.out.println("Removing exam "+tmpE.examId);
                            roomVector.get(r).allocateExam(tmpE.examId);
                            tmpExamVector.remove(tmpE);//System.out.println("tmpExamVector now has "+tmpExamVector.size()+" exams");
                            //System.out.println("Removing room "+tmpR.roomId);
                            tmpRoomVector.remove(tmpR);//System.out.println("tmpRoomVector now has "+tmpRoomVector.size()+" rooms");
                        } else {
                            r++;
                        }
                    }

//                if(tmpRoomVector.size()>0){
//                        System.out.print("For timeslot "+t+", freeRoom(s) =");
//                        int freeSpace = 0;
//                        for(int i=0;i<tmpRoomVector.size();i++)
//                        {
//                            System.out.print(tmpRoomVector.get(i).roomId+", ");
//                            int cap = tmpRoomVector.get(i).capacity;
////                            System.out.print("room size :"+cap);
//                            freeSpace+=cap;                            
//                        }
//                        System.out.println("total free Space= "+freeSpace);
//                }                                
                    if (r >= tmpRoomVector.size() && e < tmpExamVector.size()) {
//                    System.out.println("...skipping exam");//+tmpExamVector.get(e).examId+"\n");// for exam "+tmpExamVector.get(0).examId+" with "+tmpExamVector.get(0).studentsCount);
                        unAllocatedExams.add(tmpExamVector.get(e));
//                    System.out.println("Allocated = "+allocatedExams.size());                    
//                    System.out.println("Unallocated = "+unAllocatedExams.size());
                        e++;
                    }
                }

//            System.out.println("****free rooms ="+tmpRoomVector.size()); 
                ArrayList tmpFreeRooms = new ArrayList(tmpRoomVector);
                if (tmpRoomVector.size() > 0) {
                    freeTimeslotRoomMap.put(tmpT.id, tmpFreeRooms);
                }
//            System.out.println("\nfreeTimeslotRoomMap (before re-allocation): ");
//            for(Map.Entry<Integer, ArrayList> entry : freeTimeslotRoomMap.entrySet()){
//                System.out.print("Timeslot "+timeslotVector.get(entry.getKey()).id+" Free Rooms:");
//                for(int i =0;i<entry.getValue().size();i++){
//                    Room rm = (Room)entry.getValue().get(i);
//                    System.out.print(rm.roomId+", ");
//                }
//                System.out.println();
//            }                        

//            int currFreeStuds=0;
//            if(unAllocatedExams.size()>0){
//                System.out.print("unAllocatedExams= ");
//                for(int i=0;i<unAllocatedExams.size();i++)
//                {
////                    System.out.print(unAllocatedExams.get(i).examId+", ");
//                    int studs = unAllocatedExams.get(i).studentsCount;
////                    System.out.print(": no. of studs = "+studs);
//                    currFreeStuds+=studs;
//                }
//                System.out.println("total unallocated studs = "+currFreeStuds);
//            }
                mainUnAllocatedExams.addAll(unAllocatedExams);
                unAllocatedExams.clear();
            }

//        System.out.println("1ST PASS ALLOCATION");
//        for(int e =0; e<examVector.size();e++)
//        {
//            Exam tmpE = (Exam)examVector.get(e);
//            
//            if(!mainUnAllocatedExams.contains(tmpE))
//            {                            
//                System.out.println("Exam "+tmpE.examId
//                    +" has been set to room "+tmpE.room.roomId                                            
//                    +" in timeslot "+tmpE.timeslot.id);              
//            }
//        }
            //RE-ALLOCATING UNALLOCATED EXAMS - version 2
//        System.out.println("RE-ALLOCATING UNALLOCATED EXAMS");
//        System.out.println("List of 2nd pass unallocated exams:");
//        for(Exam ex:mainUnAllocatedExams){
//            System.out.print(ex.examId+" ");
//        }System.out.println();
            boolean examAllocated;
//        boolean cannotAllocateTime;
            Collections.sort(mainUnAllocatedExams, new ExamComparatorByEnrollment().reversed());
            ArrayList<Exam> pass2Unallocated = new ArrayList();
            for (Exam exam1 : mainUnAllocatedExams) {
//        for(int i=0; i<mainUnAllocatedExams.size();i++){              
//            Exam exam1 = mainUnAllocatedExams.get(0);
//            System.out.println("Attempting to allocate exam..."+exam1.examId);

//            for(Integer time : freeTimeslotRoomMap.keySet()){
//                System.out.println("Free Rooms in Timeslot "+time+": ");
//                int j=0;
//                for(ArrayList<Room> roomZ : freeTimeslotRoomMap.values()){
//                    System.out.print(roomZ.get(j++).roomId+", ");
//                }
//                System.out.println();
//            }
                ArrayList<Integer> freeTimeslots = new ArrayList(freeTimeslotRoomMap.keySet());

//            cannotAllocateTime = false;            
                examAllocated = false;
                boolean conflictFound;
                for (int newTimeslot : freeTimeslots) {
//            if(freeTimeslots.size()>0){
//                int timeslotIndex=0;
//                int timeslot = freeTimeslots.get(timeslotIndex);
//                System.out.println("\tTrying Timeslot.."+timeslot); 
                    conflictFound = false;
                    for (Exam exam2 : timeslotVector.get(newTimeslot).examList) {
//                for(int j = 0;j<examVector.size();j++){                
//                    Exam exam2 = examVector.get(j);
//                    if(timeslot!=exam2.timeslot.id)continue; 
//                    int exam1ID = exam1.examId;
//                    int exam2ID = exam2.examId;
//                    int conf = conflictMatrix[exam1ID][exam2ID];
                        if (conflictMatrix[exam1.examId][exam2.examId] != 0) {
//                        System.out.println("\tExam "+(exam1ID+1)+" conflicts with "+" Exam "+(exam2ID+1)
//                                +" @ slots "+timeslot+" & "+exam2.timeslot.id+" resp.\t");
//                        System.out.println("\tconflictMatrix["+(exam1ID+1)+"]["+(exam2ID+1)+"]= "+conf);
                            conflictFound = true;
                            break;
                        }
                    }
                    if (conflictFound) {
                        continue;
                    } else {
//                    boolean roomAllocated=false;
//                    boolean noRoom=false;
                        ArrayList<Room> freeRooms = freeTimeslotRoomMap.get(newTimeslot);
                        Collections.sort(freeRooms, new RoomComparator().reversed());
                        for (Room rm : freeRooms) {
                            Room selectedRoom = roomVector.get(rm.roomId);
                            if (exam1.studentsCount <= selectedRoom.capacity) {
//                            System.out.println("\tRoom "+rm.roomId+" is suitable.");
                                int oldTimeslot = exam1.timeslot.id;
//                            Timeslot newTimeslot = timeslotVector.get(timeslot);
                                timeslotVector.get(oldTimeslot).removeExam(exam1.examId);
                                timeslotVector.get(newTimeslot).addExam(exam1.examId);

                                examVector.get(exam1.examId).setTimeslot(newTimeslot);
                                examVector.get(exam1.examId).setRoom(rm.roomId);

                                roomVector.get(rm.roomId).allocateExam(exam1.examId);

//                            System.out.println("\tExam "+exam1.examId+" has been set to room "
//                                    +exam1.room.roomId+" @ Timeslot "+exam1.timeslot.id);
//                            freeTimeslotRoomMap.get(timeslot).remove(randRoomIndex);
                                freeTimeslotRoomMap.get(newTimeslot).remove(0);

//                            mainUnAllocatedExams.remove(exam1);
//                            System.out.println("\tRooms remaining: "+freeTimeslotRoomMap.get(timeslot).size());                            
                                if (freeTimeslotRoomMap.get(newTimeslot).size() <= 0) {
                                    freeTimeslotRoomMap.remove(newTimeslot);
//                                System.out.println("\tNo more rooms in timeslot "+timeslot+". Timeslot removed");
//                                System.out.println("\tTimeslots remaining "+freeTimeslotRoomMap.size());
                                }
                                examAllocated = true;
//                            roomAllocated=true;                            
                            }
                            if (examAllocated) {
                                break;
                            }
//                        else{
////                            System.out.println("\tRoom not suitable. ");
////                            System.out.println("\tExam "+exam1.examId+"'s enrollment = "+exam1.studentsCount+". But Room"+rm.roomId+"'s capacity = "+rm.capacity);
//                            roomIndex++;
////                            System.out.println("\tTrying again...");           
//                        }
                        }
                    }
                    if (examAllocated) {
                        break;
                    }
//                boolean roomAllocated=false;
//                boolean noRoom=false;
//                while(true){
//                    if(roomAllocated||cannotAllocateTime)break;          
//                    while(conflictFound||noRoom){
//                        noRoom=false;
//                        timeslotIndex++;
//                        
//                        if(timeslotIndex>=freeTimeslots.size()){
////                            System.out.println("\tTimeslots Exhausted. Cannot allocate exam"+exam1.examId
////                                    +". Moving to next exam");
//                            
//                            cannotAllocateTime = true;
//                            break;
//                        }
//                        else{
//                            timeslot = freeTimeslots.get(timeslotIndex);
////                            System.out.println("\tTrying next Timeslot = "+timeslot);
//                            conflictFound=false;
//                            for(int j = 0;j<examVector.size();j++){                
//                                Exam exam2 = examVector.get(j);
//                                if(timeslot!=exam2.timeslot.id)continue; 
//                                int exam1ID = exam1.examId;
//                                int exam2ID = exam2.examId;
//                                int conf = conflictMatrix[exam1ID][exam2ID];
//                                if(conf!=0){
////                                    System.out.println("\tExam "+(exam1ID+1)+" conflicts with "+" Exam "
////                                            +(exam2ID+1)+" @ slots "+timeslot+" & "+exam2.timeslot.id+" resp.\t");
////                                    System.out.println("\tconflictMatrix["+(exam1ID+1)+"]["+(exam2ID+1)+"]= "+conf);
//                                    conflictFound=true;
//                                    break;
//                                }                            
//                            }
//                        }
//                    }
////                    System.out.println("\tNo conflict found with all exams on timeslot "+timeslot);              
//                    ArrayList<Room> freeRooms = freeTimeslotRoomMap.get(timeslot);
//                    Collections.sort(freeRooms, new RoomComparator().reversed());
//                    roomAllocated =false;
//                    int roomIndex=0;
//                    while(!roomAllocated){                        
//                        if(roomIndex>=freeRooms.size()){
////                            System.out.println("\tRooms exhausted. Trying next timeslot...");
//                            noRoom=true;
//                            break;
//                        }    
//                        
//                        int selectedRoom = freeRooms.get(roomIndex).roomId;
//                        Room rm = roomVector.get(selectedRoom);
//                        if(exam1.studentsCount<=rm.capacity){
////                            System.out.println("\tRoom "+rm.roomId+" is suitable.");
//                            Timeslot oldTimeslot = exam1.timeslot;
//                            Timeslot newTimeslot = timeslotVector.get(timeslot);
//                            timeslotVector.get(oldTimeslot.id).removeExam(exam1.examId);
//                            timeslotVector.get(newTimeslot.id).addExam(exam1.examId);
//                            
//                            examVector.get(exam1.examId).setTimeslot(newTimeslot);
//                            examVector.get(exam1.examId).setRoom(rm.roomId);
//                            
//                            roomVector.get(rm.roomId).allocateExam(exam1.examId);
//                                                        
////                            System.out.println("\tExam "+exam1.examId+" has been set to room "
////                                    +exam1.room.roomId+" @ Timeslot "+exam1.timeslot.id);
////                            freeTimeslotRoomMap.get(timeslot).remove(randRoomIndex);
//                            freeTimeslotRoomMap.get(timeslot).remove(0);
//                            
//                            mainUnAllocatedExams.remove(exam1);
////                            System.out.println("\tRooms remaining: "+freeTimeslotRoomMap.get(timeslot).size());                            
//                            if(freeTimeslotRoomMap.get(timeslot).size()<=0){
//                                freeTimeslotRoomMap.remove(timeslot);
////                                System.out.println("\tNo more rooms in timeslot "+timeslot+". Timeslot removed");
////                                System.out.println("\tTimeslots remaining "+freeTimeslotRoomMap.size());
//                            }
////                            examAllocated = true;
//                            roomAllocated=true;                            
//                        }
//                        else{
////                            System.out.println("\tRoom not suitable. ");
////                            System.out.println("\tExam "+exam1.examId+"'s enrollment = "+exam1.studentsCount+". But Room"+rm.roomId+"'s capacity = "+rm.capacity);
//                            roomIndex++;
////                            System.out.println("\tTrying again...");           
//                        }
////                        if(examAllocated){
////                            break;
////                        }
//                    }
//                    if(examAllocated){
//                        break;
//                    }
//                }                
//            }                
                }
                if (!examAllocated) {
                    pass2Unallocated.add(exam1);
                }
            }

//        System.out.println("2ND PASS ALLOCATION");
//        for(int e =0; e<examVector.size();e++)
//        {
//            Exam tmpE = (Exam)examVector.get(e);
//            
//            if(!pass2Unallocated.contains(tmpE))
//            {                            
//                System.out.println("Exam "+tmpE.examId
//                    +" has been set to room "+tmpE.room.roomId                                            
//                    +" in timeslot "+tmpE.timeslot.id);                
//            }
//        }
            //SHARED ROOM ALLOCATION
//        System.out.println("THIRD PASS ROOM ALLOCATION WITH SHARED ROOMS");
            ArrayList<Exam> pass3Unallocated = new ArrayList();
//        pass3Unallocated.addAll(thirdPassUnallocated);
//        System.out.println("List of unallocated exams:");
//        for(Exam ex:pass2Unallocated){
//            System.out.print(ex.examId+" ");
//        }System.out.println();
            for (Exam exam1 : pass2Unallocated) {
                boolean allocated = false;
//            System.out.println("Third attempt to find room for \n\tExam "+exam1.examId
//                    +" previously in Timeslot "+exam1.timeslot.id
//                    +" and Room NULL");
                for (Timeslot timeS : timeslotVector) {

//                System.out.print("\tChecking Timeslot "+timeS.id+" for conflicts....");
                    boolean conflict = false;
                    for (Exam exam2 : timeS.examList) {
//                    System.out.print("\tconflict between exams "+exam1.examId+" and "+exam2.examId+"?...");
                        if (conflictMatrix[exam1.examId][exam2.examId] != 0) {
                            conflict = true;
//                        System.out.print("\tconflict between exams "+exam1.examId+" and "+exam2.examId+"?...");                      
//                        System.out.println(" = "+conflictMatrix[exam1.examId][exam2.examId]);
                            break;
                        }
//                    System.out.println("none found.");
                    }
                    if (!conflict) {
//                    System.out.println("None found in timeslot "+timeS.id);
                        ArrayList<Room> allRooms = new ArrayList();
                        allRooms.addAll(roomVector);
                        Collections.shuffle(allRooms);
                        for (Room room : allRooms) {
//                       if(room.getExams(timeS.id).size()>2)continue;
//                       System.out.print("\tIs room "+room.roomId+" suitable?....");
                            if (exam1.studentsCount <= room.getFreeSeats(timeS.id)) {
//                            System.out.println("Yes. Allocating");
                                int oldTimeslot = exam1.timeslot.id;
                                int newTimeslot = timeS.id;
                                timeslotVector.get(oldTimeslot).removeExam(exam1.examId);
                                timeslotVector.get(newTimeslot).addExam(exam1.examId);

                                examVector.get(exam1.examId).setTimeslot(newTimeslot);
                                examVector.get(exam1.examId).setRoom(room.roomId);

                                roomVector.get(room.roomId).allocateExam(exam1.examId);
//                            System.out.println("\tExam "+exam1.examId
//                                    +" now in Timeslot "+exam1.timeslot.id
//                                    +" @ Room "+exam1.room.roomId);                                                    
                                allocated = true;
                                break;
                            } else {
//                            System.out.println("No. Trying next room");
                            }
                        }
                    }
                    if (allocated) {
                        break;
                    }
//                System.out.println("\tTrying next timeslot");
                }
                if (!allocated) {
                    pass3Unallocated.add(exam1);
//                System.out.println("\tCannot allocate exam "+exam1.examId+". Must be split");
                }
            }

//        System.out.println("\n****ALLOCATION SUMMARY*****");        
//        
//        for(Timeslot tS : timeslotVector){
//            System.out.println("Timeslot "+tS.id+":");
//            for(Exam ex: tS.examList){
//                System.out.print("\tExam "+ex.examId);
//                if(ex.room==null){
//                    System.out.println(" has no room");
//                }
//                else{
//                    System.out.println(" is in room "+ex.room.roomId+" with "+(ex.room.capacity-ex.studentsCount)+" free Seats.");                    
//                }                               
//            }            
//        }
//        System.out.println("Room Vector -->");
//        for(Room r:roomVector){
//            System.out.println("Room "+r.roomId);
//            for(Timeslot t:timeslotVector){
//                System.out.print("\tExams in Timeslot "+t.id+": ");
//                for(Exam e:r.examList){
//                    if(e.timeslot.id==t.id)System.out.print(e.examId+" ");
//                }
//                System.out.println(".");
//            }            
//        }
//        System.out.println("\nfreeTimeslotRoomMap (after reallocation) size = "+freeTimeslotRoomMap.size());
//        for(Map.Entry<Integer, ArrayList> entry : freeTimeslotRoomMap.entrySet()){
//            System.out.print("Timeslot "+timeslotVector.get(entry.getKey()).id+" Free Rooms:");
//            for(int i =0;i<entry.getValue().size();i++){
//                Room rm = (Room)entry.getValue().get(i);
//                System.out.print(rm.roomId+", ");
//            }
//            System.out.println();
//        } 
//        System.out.println("3RD PASS ALLOCATION");
            int freeSeats = 0;
            for (int e = 0; e < examVector.size(); e++) {
                Exam tmpE = (Exam) examVector.get(e);

                if (!pass3Unallocated.contains(tmpE)) {
                    int seats = tmpE.room.capacity - tmpE.studentsCount;
//                System.out.println("Exam "+tmpE.examId
//                    +" has been set to room "+tmpE.room.roomId                                            
//                    +" in timeslot "+tmpE.timeslot.id
//                    +" with "+(seats)+" free seats");
                    freeSeats += seats;
                }
            }

//        System.out.println("UNALLOCATED EXAMS:");
//        Collections.sort(pass3Unallocated, new ExamComparator().reversed());
//        int unAllocatedStudents=0;
//        for(int i=0;i<pass3Unallocated.size();i++){
////            System.out.println((i+1)+" - Exam: "+pass3Unallocated.get(i).examId+". Enrollment = "+pass3Unallocated.get(i).studentsCount);
//            unAllocatedStudents+=pass3Unallocated.get(i).studentsCount;
//        }
////        System.out.println("UNALLOCATED EXAMS = "+pass3Unallocated.size());
////        System.out.println("TOTAL UNALLOCATED STUDENTS = "+unAllocatedStudents);
////        System.out.println("TOTAL UNUSED SEATS = "+freeSeats); 
            return true;
        } catch (Exception e) {
            System.out.println("Cannot Allocate Rooms");
            return false;
        }
    }

    void roomAllocation(){
//        System.out.println("Room Allocation Module");
//        IntegerMatrixSolution solution = new DefaultIntegerMatrixSolution(getListOfExamsPerVariable(), getNumberOfObjectives());
//        PriorityQueue<Room> sortedRooms = new PriorityQueue(numberOfRooms, new RoomComparatorByDistance());
//        ArrayList<Integer> randRooms = new ArrayList<>();
//        ArrayList<Student> examEnrollment = new ArrayList<>();
        
        for(Department dept: departmentVector){
            ArrayList<Room> rooms = new ArrayList<>();
            rooms.addAll(roomVector);
            for(Room rm:rooms){
                rm.setDistanceToDept(dept.longitude, dept.latitude);
            }
            dept.roomsQueue.addAll(rooms);
        }
        
//        Collections.shuffle(randRooms);
        for(Exam ex:examVector){
            
            for(Student std:ex.enrollmentList){
                std.setRoom(-1);
            }
            
            for(Room rm:roomVector){
                rm.studentList.clear();
            }
            
//            System.out.println("\tAllocating Exam "+ex.examId+" @ timeslot "+ex.timeslot.id+" to rooms");
//            System.out.println("\tNumber of students = "+ex.enrollmentList.size());
            
//            sortedRooms.clear();
//            sortedRooms.addAll(roomVector);
//            examEnrollment.addAll(ex.enrollmentList);
//            ArrayList<Room> roomsInList = new ArrayList<>();
            
//            for(int i=0;i<roomsInList.size();i++){
//                Room head = sortedRooms.poll();    
//                roomsInList.add(i,head);
//                
//                for(Department dept : departmentVector){                    
//                    for(Room rm:roomsInList){   
//                        rm.setDistanceToDept(dept.longitude, dept.latitude);
//                    }                                        
//                }
//            }
            
            for(Student std:ex.enrollmentList){
                
                Department currentDept = departmentVector.get(std.deptId);
//                System.out.println("\t\tAttempting to place student "+std.sId+" from dept "+std.deptId);
                
                while(std.roomId==-1){
                    if(currentDept.roomsQueue.isEmpty()){
                        System.out.println("\t\t\tCan't Allocate Student. Rooms Exhausted...breaking to next student");
                        break;
                    }
//                    System.out.println("\t\t\t"+currentDept.roomsQueue.size()+" rooms remaining");
                    Room currentRoom = currentDept.roomsQueue.peek();
//                    System.out.println("\t\t\tClosest Room ="+currentRoom.roomId+". Capacity = "+currentRoom.capacity+". Free Seats = "+(currentRoom.capacity-currentRoom.studentList.size()));
                    if(currentRoom.studentList.size()<currentRoom.capacity){
                        
                        std.setRoom(currentRoom.roomId);
                        currentRoom.allocateStudent(std);
                        ex.setRoom(currentRoom.roomId);
//                        timetableSolution.get(ex.examId).set(ex.timeslot.id, currentRoom.roomId);
//                        solution.setVariable(ex.examId, timetableSolution.get(ex.examId));
//                        System.out.println("\t\t\tStudent "+std.sId+" placed in room "+currentRoom.roomId+". Moving to next student");
                    }
                    else{
//                        System.out.println("\t\t\tRoom "+currentRoom.roomId+" filled. Moving next room...");
                        currentDept.roomsQueue.remove(currentRoom);
                    }
                }                
            }
        }
    }
    
    ArrayList<ArrayList<Integer>> generateTimeTableMatrix() {
//        ArrayList<Integer> randRooms = new ArrayList<>();
//        for (int i = 0; i < numberOfRooms; i++) {
//            randRooms.add(i);
//        }
        feasible = allocateTimeslots();
//        feasible = allocateTimeslotsByDifficulty();
        
//        allocateRooms(randRooms);

        ArrayList<Integer> tmpSlots = new ArrayList<>();
        for (int i = 0; i < numberOfTimeslots; i++) {
            tmpSlots.add(i, -1);
        }
        
        timetableSolution.clear();
        
        for (int j = 0; j < numberOfExams; j++) {
            timetableSolution.add(j, new ArrayList<>(tmpSlots));
//            System.out.println("Timetable solution ("+j+") = "+timetableSolution.get(j).toString());
        }
        
        roomAllocation();
        
        for (int i = 0; i < numberOfExams; i++) {
            for (int j = 0; j < numberOfTimeslots; j++) {
                if (examVector.get(i).timeslot.id != j) {
                    continue;
                }
                
                int room = -1;
                if (examVector.get(i).room != null) {
                    room = examVector.get(i).room.roomId;
                }
//                System.out.println("OLDtimetableSolution.get("+i+")="+timetableSolution.get(i).toString());
//                System.out.println("\tSetting room of exam "+i+" to "+room+" @ timeslot "+j);
                timetableSolution.get(i).set(j, room);
//                System.out.println("NEWtimetableSolution.get("+i+")="+timetableSolution.get(i).toString());
            }
        }
//        System.out.println("timetableSolution = "+timetableSolution);
//        timetableSolution = 
        return timetableSolution;
    }

    IntegerMatrixSolution iteraiveForwardSearch() {
        //Declare collections
        IntegerMatrixSolution solution = new DefaultIntegerMatrixSolution(getListOfExamsPerVariable(), getNumberOfObjectives());           
        PriorityQueue<Exam> unAllocatedExams = new PriorityQueue<>(numberOfExams, new ExamComparatorByRatio());   
//        PriorityQueue<Exam> unAllocatedExams = new PriorityQueue<>(numberOfExams, new ExamComparatorByEnrollment());  
        //Initilaize solution
        for(int i=0; i<solution.getNumberOfVariables();i++){
            ArrayList<Integer> examArray = new ArrayList<>(numberOfTimeslots);
            for(int j=0; j<numberOfTimeslots;j++){
                examArray.add(-1);
            }
            solution.setVariable(i, examArray);
        }
        
            
        //Initialize and Populate all exams with timeslot-room values
        for(Exam e:examVector){
            //Reset exam priorities            
            e.timeslotRoomQueue.clear();
            for(int i = 0; i< numberOfTimeslots;i++){
                for(int j=0; j<numberOfRooms;j++){
                    if(e.studentsCount>roomVector.get(j).capacity)continue;
                    e.timeslotRoomQueue.add(new TimeslotRoom(i,j));                                        
                }
            }
            e.setDSizeToHCRatio();
        }
        
        //Reset exam list of all rooms
        for(Room r:roomVector)r.examList.clear();
        
        //Reset exam list of all timeslots
        for(Timeslot t:timeslotVector)t.examList.clear();
        
        //Populate exam priority queue
        unAllocatedExams.addAll(new ArrayList(examVector));
        //Allocate exams if any
        while(unAllocatedExams.size()>0){                
                //Collect all minimum priority exams and select one randomly
                ArrayList<Exam> tmpUnallocatedExams = new ArrayList<>();
                int minPriority = unAllocatedExams.peek().dSizeToHCRatio;
    //            System.out.println("\nunAllocatedExams.peek().dSizeToHCRatio = "+minPriority);
    //            int lineCounter=0;
    //            System.out.println("Randomly selecting from Unallocated Exams with dSizeToHCRatio < = "+minPriority);           
                for(Exam e:unAllocatedExams){
    //                System.out.print(e.examId+".dSizeToHCRatio = "+e.dSizeToHCRatio+"\t\t");                
                    if(e.dSizeToHCRatio <= minPriority)tmpUnallocatedExams.add(e);   
    //                if(++lineCounter%5==0)System.out.println();                
                }
    //            int lineCounter=0;
    //            System.out.println("Randomly selecting from Unallocated Exams with dSizeToHCRatio < = "+minPriority);
    //            for(Exam e:tmpUnallocatedExams){
    //               System.out.print(e.examId+". dSizeToHCRatio = "+e.dSizeToHCRatio+"\t");
    //               if(++lineCounter%7==0)System.out.println();
    //            }

                Exam selectedExam = tmpUnallocatedExams.get(new Random().nextInt(tmpUnallocatedExams.size()));
                int selectedExamID = selectedExam.examId;

    //            System.out.println("\nSelected Exam "+selectedExamID+"...");
    //            System.out.println("\nRanking TimeslotRoom values for exam "+selectedExamID);  
                ArrayList<TimeslotRoom> tmpQueue = new ArrayList<>(selectedExam.timeslotRoomQueue);
                selectedExam.timeslotRoomQueue.clear();
                int newLine=0;
                for(int j =0; j<tmpQueue.size();j++){                
                    TimeslotRoom tr = tmpQueue.get(j);
    //                if(selectedExam.timeslot.id==tr.timeslot&&selectedExam.room.roomId==tr.room){
    //                    System.out.println("\toldTimeslotRoom.");
    //                    continue;
    //                }
    //                for(Conflict con:tr.conflicts){
    //                    if(con.conflictingExam==selectedExam){
    //                        System.out.println("\tHas been evicted from ("+tr.timeslot+", "+tr.room+") "+con.evictionCount+" times");
    //                        tr.possibleConflicts+=con.evictionCount;                        
    //                    }
    //                }

                    Room room = roomVector.get(tr.room);
                    Timeslot timeslot = timeslotVector.get(tr.timeslot);
                    if(room.getFreeSeats(timeslot.id)<selectedExam.studentsCount){                    
                        for(Exam e:room.examList){
    //                        if(e.examId==selectedExamID){
    //                            System.out.println("\tskipping same exam");
    //                            continue;
    //                        }
                            if(e.timeslot.id!=tr.timeslot)continue;                                                   
                            if (e.getTimeslotRoom(tr).conflicts.isEmpty()) {
                                tr.possibleConflicts++;
    //                            System.out.println("\n\t\tExam  "+selectedExamID+" has a probable Full Room conflict with NEW exam "+e.examId+
    //                                            " @ ("+e.getTimeslotRoom(tr).timeslot+", "+e.getTimeslotRoom(tr).room+"). tr.possibleConflicts = "+tr.possibleConflicts);
                            } else {
                                for(Conflict con : e.getTimeslotRoom(tr).conflicts){
                                    if(con.conflictingExam == selectedExam){
                                       tr.possibleConflicts+=con.evictionCount;
    //                                    System.out.println("\t\tExam  "+selectedExamID+" has a probable Full Room conflict with OLD exam "+con.conflictingExam.examId+
    //                                            " @ ("+e.getTimeslotRoom(tr).timeslot+", "+e.getTimeslotRoom(tr).room+"). tr.possibleConflicts = "+tr.possibleConflicts);
    //                                    System.out.println("\t\tTimeslotRoom rank = "+tr.rank);
                                    }
                                }
                            }                                                        
                        }                        
                    }
                    else{
    //                    System.out.println("\tSquatting possible in room "+room.roomId+" with "+room.examList.size()+" exams.");
                        for(Exam e:room.examList){
    //                        if(e.examId==selectedExamID){
    //                            System.out.println("\tskipping same exam");
    //                            continue;
    //                        }
    //                        System.out.print("\tChecking for conflicts with exam "+e.examId+" having timeslot "+e.timeslot.id+"...");
                            if(e.timeslot.id!=tr.timeslot){
    //                            System.out.println("different timeslot");
                                continue;
                            }
                            if(conflictMatrix[e.examId][selectedExamID]!=0){
    //                           System.out.print("found.\n\tConflict exists previously?...");
                                if (e.getTimeslotRoom(tr).conflicts.isEmpty()) {
    //                                System.out.println("No");
                                    tr.possibleConflicts++;
    //                                System.out.println("\n\t\tExam  "+selectedExamID+" has a probable Partial Room conflict with NEW exam "+e.examId+
    //                                            " @ ("+e.getTimeslotRoom(tr).timeslot+", "+e.getTimeslotRoom(tr).room+"). tr.possibleConflicts = "+tr.possibleConflicts);
                                } else {
    //                                System.out.println("Yes");
                                    for(Conflict con : e.getTimeslotRoom(tr).conflicts){
                                        if(con.conflictingExam == selectedExam){
                                            tr.possibleConflicts += con.evictionCount;
    //                                        System.out.println("\t\tExam  "+selectedExamID+" has a probable Partial Room conflict with OLD exam "+con.conflictingExam.examId+
    //                                            " @ ("+e.getTimeslotRoom(tr).timeslot+", "+e.getTimeslotRoom(tr).room+"). tr.possibleConflicts = "+tr.possibleConflicts);
    //                                        System.out.println("\t\tTimeslotRoom rank = "+tr.rank);
                                        }
                                    }  
                                }                                                                
                            }
                            else{
    //                            System.out.println("Not found");
                            }
                        }
                    }
    //                }
                    int prevEvictionCount = 0; 
                    for(int i=0; i<solution.getNumberOfVariables();i++){
                        if(examVector.get(i).getTimeslotRoom(tr)==null)continue;
                        ArrayList<Integer> tmpExam = (ArrayList)solution.getVariable(i);                    
                        int tSlot = getTimeslot(tmpExam);                                        
    //                    System.out.print("\tIs exam "+selectedExamID+" @ timeslot "+tr.timeslot);
    //                    System.out.print(" in the same timeslot with Exam "+i+" @ timeslot "+tSlot+"?");
                        if(tSlot==timeslot.id){
    //                        System.out.println("...Yes");
    //                        System.out.print("\t\tIs there conflict?");
                            if (conflictMatrix[i][selectedExamID]!=0) {
    //                            System.out.print("...Yes. "+conflictMatrix[i][selectedExamID]+" student(s)\n\t\tHas conflict occured previouly? ");                            
                                if(examVector.get(i).getTimeslotRoom(tr).conflicts.isEmpty()){
    //                                System.out.println("...No");
    //                                tr.possibleConflicts++; 
                                    for(TimeslotRoom tmR: tmpQueue){
                                        if(tmR.timeslot==tr.timeslot){
        //                                    System.out.println("\t\t\tUpdating timeslot ("+tmR.timeslot+", "+tmR.room+") for possible conflict with exam "+i);
                                            tmR.possibleConflicts++;
                                        }
                                    }   
    //                                System.out.println("\n\t\tExam "+selectedExamID+" has a probable Timeslot conflict with NEW exam "+i+
    //                                                " @ ("+examVector.get(i).getTimeslotRoom(tr).timeslot+", "+examVector.get(i).getTimeslotRoom(tr).room+"). tr.possibleConflicts = "+tr.possibleConflicts);
                                }
                                else{
    //                                System.out.println("...Yes.\n\t\tLocating previous conflict...");

                                    for(Conflict con : examVector.get(i).getTimeslotRoom(tr).conflicts){
    //                                    System.out.print("\t\tIs conflict with exam "+con.conflictingExam+" and evictionCount = "+con.evictionCount+" the target?");
                                        if(con.conflictingExam == selectedExam){
    //                                        System.out.println("\t\t...Yes");

                                            for(TimeslotRoom tmR: tmpQueue){
                                                if(tmR.timeslot==tr.timeslot){
    //                                                System.out.println("\t\t\tUpdating timeslot ("+tmR.timeslot+", "+tmR.room+") for possible conflict with exam "+i);
    //                                                int rnkA = tr.possibleConflicts+con.evictionCount;
    //                                                int rnkM = tr.possibleConflicts*con.evictionCount;
    //                                                System.out.println("\t\t\trnkAdd = "+rnkA);
    //                                                System.out.println("\t\t\trnkMult = "+rnkM);
    //                                               prevEvictionCount = con.evictionCount;
                                                   tr.possibleConflicts+=con.evictionCount;
                                                }
                                            }
    //                                        System.out.println("\t\tExam "+selectedExamID+" has a probable Timeslot conflict with OLD exam "+con.conflictingExam.examId+
    //                                                " @ ("+examVector.get(i).getTimeslotRoom(tr).timeslot+", "+examVector.get(i).getTimeslotRoom(tr).room+"). tr.possibleConflicts = "+tr.possibleConflicts);
    //                                        System.out.println("\t\tTimeslotRoom rank = "+tr.rank);
                                        }
                                        else{
    //                                        System.out.println("...No");
                                        }

                                    }
                                }

    //                            for(TimeslotRoom tmR: tmpQueue){
    //                                if(tmR.timeslot==tr.timeslot){
    ////                                    System.out.println("\t\t\tUpdating timeslot ("+tmR.timeslot+", "+tmR.room+") for possible conflict with exam "+i);
    //                                    tmR.possibleConflicts++;
    //                                }
    //                            }
                            }
                            else{
    //                            System.out.println("...No");
                            }
                        }
                        else{
    //                        System.out.println("...No");
                        }  
                    }                                

                    tr.rank += tr.possibleConflicts;
                    System.out.print("\t("+tr.timeslot+", "+tr.room+").rank = "+tr.rank);
                    if(++newLine%9==0)System.out.println();
                    selectedExam.timeslotRoomQueue.offer(tr);
                }
    //            System.out.println();
//            }
//            else{
//                System.out.println("\t\tChanging timeslot. Higher Exam Met");
//            }
                //Collect all minimum priority timeslot-room values and select one randomly
                ArrayList<TimeslotRoom> tmpTimeslotRoomQueue = new ArrayList<>();
                long minRank = selectedExam.timeslotRoomQueue.peek().rank;
    //            System.out.println("\tselectedExam.timeslotRoom.peek().rank = "+selectedExam.timeslotRoomQueue.peek().rank);
                for(TimeslotRoom tr:selectedExam.timeslotRoomQueue){
    //                System.out.println("\t\t("+tr.timeslot+", "+tr.room+"). rank = "+tr.rank);
                    if(tr.rank<=minRank){
                        tmpTimeslotRoomQueue.add(tr);                    
                    }
                }
            

            TimeslotRoom selectedTimeslotRoom = tmpTimeslotRoomQueue.get(new Random().nextInt(tmpTimeslotRoomQueue.size()));
//            System.out.println("\tselectedTimeslotRoom ("+selectedTimeslotRoom.timeslot+", "+selectedTimeslotRoom.room+")'s rank = "+selectedTimeslotRoom.rank);
//            TimeslotRoom tmpSelectedTimeslotRoom = tmpTimeslotRoomQueue.get(new Random().nextInt(tmpTimeslotRoomQueue.size()));
//            TimeslotRoom selectedTimeslotRoom = new TimeslotRoom(tmpSselectedTimeslotRoom);            
//            selectedExam.timeslotRoomQueue.remove(tmpSelectedTimeslotRoom);
            
            System.out.println("\n\tAllocating Exam "+selectedExamID+" with rank "+ selectedExam.dSizeToHCRatio+" to TimeslotRoom ("+selectedTimeslotRoom.timeslot+", "+selectedTimeslotRoom.room+").rank =  "+selectedTimeslotRoom.rank
                    + " ["+unAllocatedExams.size()+" exams remaining.]");  
//            System.out.println(" AllocationCount = "+selectedExam.allocationCount+". Deallocation Count = "+selectedExam.deAllocationCount);

            int selectedTimeslotID = selectedTimeslotRoom.timeslot;
            int selectedRoomID = selectedTimeslotRoom.room;
            Timeslot selectedTimeslot = timeslotVector.get(selectedTimeslotID);
            Room selectedRoom = roomVector.get(selectedRoomID);
                        
            selectedExam.setTimeslot(selectedTimeslotID);
            selectedExam.setRoom(selectedTimeslotRoom.room); 
            examVector.get(selectedExamID).setTimeslot(selectedTimeslotID);
            examVector.get(selectedExamID).setRoom(selectedRoomID);
            
            ArrayList<Integer> currExam = new ArrayList<>((ArrayList)solution.getVariable(selectedExamID));
            currExam.set(selectedTimeslotID, selectedRoomID);
            solution.setVariable(selectedExamID, currExam);
            
            //Check for room feasibility and conflict with existing assignment
            //and evict all necessary exams  
            
            if (selectedRoom.getFreeSeats(selectedTimeslotID) < selectedExam.studentsCount) {                 
                //Evict existing exams from selectedRoom
                ArrayList<Exam> examsInRoom = new ArrayList<>(selectedRoom.examList);
                for(int i=0;i<examsInRoom.size();i++){                
//                while(selectedRoom.examList.size()>0){
                    int examIndex = examsInRoom.get(i).examId;
                    Exam conflictingExam = examVector.get(examIndex);
                    if(conflictingExam.timeslot.id!=selectedTimeslotID)continue;
                    int conflictingExamID = conflictingExam.examId;
                    //Deallocate conflictingExam in selectedRoom
                    ArrayList<Integer> tmpExam = new ArrayList<>((ArrayList)solution.getVariable(conflictingExamID));
                    int tSlot = getTimeslot(tmpExam);
                    int room = getRoom(tmpExam);
                    
                    tmpExam.set(tSlot, -1);
                    solution.setVariable(conflictingExamID, tmpExam);

                    conflictingExam.setRoom(-1);
                    conflictingExam.setTimeslot(-1);
//                        conflictingExam.priority-=2;
//                    conflictingExam.setDSizeToHCRatio();

//                        TimeslotRoom conflictingTimeslotRoom = conflictingExam.getTimeslotRoom(selectedTimeslotRoom);
                    TimeslotRoom conflictingTimeslotRoom = conflictingExam.getTimeslotRoom(tSlot, room);
                    System.out.println("\t\tRemoved exam "+conflictingExamID+" in ("+tSlot+", "+room+").rank = "+conflictingTimeslotRoom.rank+" due to Insufficient Room Capacity.");
//                    System.out.println("\tconflictingTimeslotRoom = ("+conflictingTimeslotRoom.timeslot+", "+conflictingTimeslotRoom.room+")");
                    boolean conflictExists = false;                        
                    for(Conflict con : conflictingTimeslotRoom.conflicts){
                        if(con.conflictingExam == selectedExam){
                            con.evictionCount++;                            
                            conflictExists = true;
//                            System.out.println("\t\tcon.evictionCount Updated = "+con.evictionCount); 
                        }
                    }
                    if(!conflictExists){                            
                        conflictingTimeslotRoom.conflicts.add(new Conflict(selectedExam,1));                            
                    }                                                                        

                    selectedRoom.deAllocateExam(conflictingExamID);
                    selectedTimeslot.removeExam(conflictingExamID);

                    unAllocatedExams.offer(conflictingExam);                                            
                }
            }else{
                //Free Seats available. Check for conflict with existing exams                    
                ArrayList<Integer> toRemove = new ArrayList<>();                                        
                for(int i=0;i<selectedRoom.examList.size();i++){
                    if(selectedRoom.examList.get(i).timeslot!=selectedExam.timeslot)continue;
                    if(conflictMatrix[selectedRoom.examList.get(i).examId][selectedExamID]!=0){
//                            System.out.println("\t\tAdding i = "+i+"to toRemove");
                        toRemove.add(selectedRoom.examList.get(i).examId);                            
                    }   
                }
                for(Integer index:toRemove){
                    //Deallocate conflictingExam in selectedRoom                        
                    Exam conflictingExam = examVector.get(index);
                    int conflictingExamID = conflictingExam.examId;
                    ArrayList<Integer> tmpExam = new ArrayList<>((ArrayList)solution.getVariable(conflictingExamID));
                    int tSlot = getTimeslot(tmpExam);
                    int room = getRoom(tmpExam);
                    
                    tmpExam.set(tSlot, -1);                        
                    solution.setVariable(conflictingExamID, tmpExam);

                    conflictingExam.setRoom(-1);
                    conflictingExam.setTimeslot(-1);
                    
//                        conflictingExam.priority-=2;
//                    conflictingExam.setDSizeToHCRatio();

//                        TimeslotRoom conflictingTimeslotRoom = conflictingExam.getTimeslotRoom(selectedTimeslotRoom);
                    TimeslotRoom conflictingTimeslotRoom = conflictingExam.getTimeslotRoom(tSlot, room);
                    System.out.println("\t\tRemoved exam "+conflictingExamID+" in ("+tSlot+", "+room+").rank ="+conflictingTimeslotRoom.rank+" due to Room Conflict.");
//                        System.out.println("\tconflictingTimeslotRoom = ("+conflictingTimeslotRoom.timeslot+", "+conflictingTimeslotRoom.room+")");
                    boolean conflictExists = false;                        
                    for(Conflict con : conflictingTimeslotRoom.conflicts){
                        if(con.conflictingExam == selectedExam){
                            con.evictionCount++;                            
                            conflictExists = true;
//                            System.out.println("\t\tcon.evictionCount Updated = "+con.evictionCount); 
                        }
                    }
                    if(!conflictExists){                            
                        conflictingTimeslotRoom.conflicts.add(new Conflict(selectedExam,1));                        
//                        conflictingTimeslotRoom.rank = 
                    }
//                        conflictingTimeslotRoom.computeRank();

                    selectedRoom.deAllocateExam(conflictingExamID);
                    selectedTimeslot.removeExam(conflictingExamID);

                    unAllocatedExams.offer(conflictingExam);
                }
            }                          
            
//            System.out.println("\tDeallocating all TimeslotConflict Exams: ");
            for(int i=0; i<solution.getNumberOfVariables();i++){
                ArrayList<Integer> tmpExam = new ArrayList<>((ArrayList)solution.getVariable(i));                
                int tmpExamRoom = getRoom(tmpExam);
                int tmpExamTimeslot = getTimeslot(tmpExam);
//                System.out.println("\ttmpExam "+i+" = "+tmpExam+". Timeslot = "+tmpExamTimeslot+". Room = "+tmpExamRoom);
                if(tmpExamTimeslot == selectedTimeslotID){
//                    System.out.print("\t\tExams "+i+" & selectedExam "+selectedExam.examId+" have same timeslot. Checking conflict....");                        
                    if(conflictMatrix[i][selectedExamID]!=0){
                        //De-allocate conflicting exam
//                        System.out.println("found.");
                        
                        tmpExam.set(selectedTimeslotID, -1);
                        solution.setVariable(i, tmpExam);
//                        System.out.println("\tDeallocated from solution. Exams "+i+" now = "+solution.getVariable(i));
                        Exam conflictingExam = examVector.get(i);
                        conflictingExam.setRoom(-1);
                        conflictingExam.setTimeslot(-1);
//                        conflictingExam.priority-=2;
//                        conflictingExam.setDSizeToHCRatio();
                        
//                        TimeslotRoom conflictingTimeslotRoom = conflictingExam.getTimeslotRoom(selectedTimeslotRoom);
                        TimeslotRoom conflictingTimeslotRoom = conflictingExam.getTimeslotRoom(tmpExamTimeslot, tmpExamRoom);
                        System.out.println("\t\tRemoved exam "+i+" in ("+tmpExamTimeslot+", "+tmpExamRoom+").rank = "+conflictingTimeslotRoom.rank+"  due to Timeslot conflict.");
//                        System.out.println("\tconflictingTimeslotRoom = ("+conflictingTimeslotRoom.timeslot+", "+conflictingTimeslotRoom.room+")");
                        boolean conflictExists = false;                        
                        for(Conflict con : conflictingTimeslotRoom.conflicts){
                            if(con.conflictingExam == selectedExam){
                                con.evictionCount++;
                                conflictExists = true;
//                                System.out.println("\t\tcon.evictionCount Updated = "+con.evictionCount);
                            }
                        }
                        if(!conflictExists){                            
                            conflictingTimeslotRoom.conflicts.add(new Conflict(selectedExam,1));                               
                        }
//                        conflictingTimeslotRoom.computeRank();
                        
//                        System.out.println("\tRemoving exam "+i+"("+solution.getVariable(i)+") due to Timeslot Conflict from room and timeslot Vectors");                        
                        roomVector.get(tmpExamRoom).deAllocateExam(i);  
                        timeslotVector.get(tmpExamTimeslot).removeExam(i);                        
                             
                        unAllocatedExams.offer(conflictingExam);
                    }
                    else{
//                        System.out.println("None");
                    }
                }                
            }
            
            //Allocation succesful. Remove selectedExam from Unallocated List 
            selectedTimeslot.addExam(selectedExamID);
            selectedRoom.allocateExam(selectedExamID);
            unAllocatedExams.remove(selectedExam);
            
//            for(Exam e:unAllocatedExams)e.priority--;
//            System.out.println("\tExam "+selectedExam.examId+" = "+solution.getVariable(selectedExam.examId));
        } 
        System.out.println("\n--->Begin Allocation Summary<---");
        for(Exam e:examVector){
            System.out.println("Exam "+e.examId+" is in timeslot "+e.timeslot.id+" and room "+e.room.roomId);
        }
        System.out.println("--->End Allocation Summary<---\n");
        return solution;
    }

    @Override
    public IntegerMatrixSolution<ArrayList<Integer>> createSolution() {
        timetableSolution = generateTimeTableMatrix();
        
        
//        for(Exam ex:examVector){
//            timetableSolution.get(ex.examId).set(ex.timeslot.id, ex.room.roomId);
//        }
        
        if (feasible) {
            DefaultIntegerMatrixSolution solution = new DefaultIntegerMatrixSolution(getListOfExamsPerVariable(), getNumberOfObjectives());
//          System.out.println("Creating solution...");
            for (int i = 0; i < getLength(); i++) {
                solution.setVariable(i, timetableSolution.get(i));
            }
//              System.out.println("new solution:"+solution.getVariables());
            return solution;
//        return iteraiveForwardSearch();
        } else {
            return null;
        }
    }

    @Override
    public void evaluate(IntegerMatrixSolution<ArrayList<Integer>> solution) {
//        double proximityFitness = evaluateProximityFitness(solution);
//        double movementFitness = evaluateMovementFitness(solution);                 //movement cost
        int difficultyFitness = evaluateDifficultyFitness(solution); 
//        double roomUtilizationFitness = evaluateRoomUtilizationFitness(solution);   //room cost

//        double compositeFitness = proximityFitness + movementFitness + roomUtilizationFitness;
//        int itc2007Fitness = evaluateITC2007Fitness(solution);

        this.evaluateConstraints(solution);
        solution.setObjective(0, difficultyFitness);
//        solution.setObjective(1, movementFitness);
//        solution.setObjective(0, roomUtilizationFitness); 
//        solution.setObjective(0, compositeFitness);
//        solution.setObjective(0, itc2007Fitness);
//        solution.setObjective(1, compositeFitness);

        System.out.println(solution.getObjective(0));
//        System.out.println("Objective(1) movementFitness =" + solution.getObjective(1));
//        System.out.println("Conflicts = " + solution.getAttribute("CONFLICTS"));
    }

    public double evaluateProximityFitness(IntegerMatrixSolution<ArrayList<Integer>> solution) {
        //proximity constraint 
        double proximity = 0.0;
        for (int i = 0; i < solution.getNumberOfVariables(); i++) {
            int slot1 = getTimeslot(solution.getVariable(i));
            if (slot1 == -1) {
                return Integer.MAX_VALUE;
            }
            for (int j = 0; j < solution.getNumberOfVariables(); j++) {
                if (i == j) {
                    continue;
                }
                int slot2 = getTimeslot(solution.getVariable(j));
                if (slot2 == -1) {
                    return Integer.MAX_VALUE;
                }
                if (conflictMatrix[i][j] != 0) {
                    double prox = Math.pow(2, (5 - Math.abs(slot1 - slot2)));
                    double diffFactor = computedDifficulty.get(i) + computedDifficulty.get(j);
                    proximity += (prox / diffFactor) * conflictMatrix[i][j];
                }
            }
        }
        return proximity / studentMap.size();
    }

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
    
    public int evaluateDifficultyFitness(IntegerMatrixSolution<ArrayList<Integer>> solution) {
        int fitness = 0;
//        System.out.println("\nEVALUATING "+studentMap.size()+" STUDENTS\n");
            for(Student std: studentMap.values()){
//                System.out.println("Student: "+std.sId+". "+std.examList.size()+" exams");
                ArrayList<Integer> allocatedTimeslots = new ArrayList<>();
                for(int i=0;i<numberOfTimeslots;i++){
                    allocatedTimeslots.add(-1);
                }
                
                for(int e=0;e<solution.getNumberOfVariables();e++){
                    Exam exm = examVector.get(e);                    
                    if(std.examList.contains(exm)){
//                        System.out.println("std.examList contains exam "+std.examList.c);
                        int time = getTimeslot(solution.getVariable(e));
                        allocatedTimeslots.set(time,e);
//                        System.out.println("\tExam "+e+" @ timeslot "+time);
                    }
                }
//                System.out.println("Timeslot list"+allocatedTimeslots.toString());
                
                for(int i=0; i<numberOfTimeslots-1;i++){
                    int e1 = allocatedTimeslots.get(i);
                    if(e1==-1)continue;
                    
                    int e2=-1;
                    for(int j=i+1; j<numberOfTimeslots;j++){
                        e2 = allocatedTimeslots.get(j);
                        if(e2!=-1)break;
                    }
                    
                    if(e2==-1)break;
                    
                    
                    int diff1 = examVector.get(e1).difficulty;
                    int diff2 = examVector.get(e2).difficulty;
//                    System.out.println("\tExam "+e1+"("+diff1+") and Exam "+e2+" ("+diff2+")");
//                    System.out.println("Is "+diff1+" == "+diff2+"?");
                    if(diff1==1&&diff2==1){
//                        System.out.println("\tYup");
                        fitness++;
                    }
                    else{
//                        System.out.println("Nope");
                    }
                    
                }
            }
            
//            System.out.println("\nEVALUATION SUCCESSFUL");
        return fitness;
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
//        System.out.println("evaluating Constraints for solution: "+solution.getVariables().toString());
        int conflicts = 0;
        for (int i = 0; i < solution.getNumberOfVariables(); i++) {
            for (int j = 0; j < solution.getNumberOfVariables(); j++) {
                if (i == j) {
                    continue;
                }
//                System.out.println("Checking conflict in exams "+examVector.get(i).examId+" and "+examVector.get(j).examId);
//                System.out.println("@ original timeslots "+examVector.get(i).timeslot.id+" and "+examVector.get(j).timeslot.id);
                int slot1 = -1, slot2 = -2;

                ArrayList<Integer> x = solution.getVariable(i);
//                System.out.println("\texam x = "+x.toString());
                slot1 = getTimeslot(x);
//                while (x.get(slot1) == 0) {
//                    slot1++;
//                }

                ArrayList<Integer> y = solution.getVariable(j);
//                System.out.println("\texam y = "+y.toString());
                slot2 = getTimeslot(y);
//                while (y.get(slot2) == 0) {
//                    slot2++;
//                }
                if (slot1 == -1 || slot2 == -1) {
                    conflicts = Integer.MAX_VALUE;
//                    System.out.println("\tRoom not assigned");
                    break;
                }
                if (conflictMatrix[i][j] != 0) {
                    if (slot1 == slot2) {
                        conflicts++;
//                        System.out.println("\tExam "+i+" conflicts with "+j+" both @ timeslot "+slot1+" & "+slot2);
//                        System.out.println("Conflicting students = "+conflictMatrix[i][j]);
                    }
                }
                else{
//                    System.out.println("\tNo conflict");
                }
            }
        }//System.out.println("conflicts="+conflicts);

        //room under-utilization constraint
        int roomOccupancyViolation = 0;
        int examCapacity = 0;
        int roomCapacity = 0;

        for (int i = 0; i < solution.getNumberOfVariables(); i++) {
            ArrayList<Integer> exam = solution.getVariable(i);

            for (int j = 0; j < exam.size(); j++) {
                if (exam.get(j) == 0) {
                    continue;
                }
                int room = exam.get(j);

                if (room != -1) {
                    examCapacity = examVector.get(i).studentsCount;
                    roomCapacity = roomVector.get(room - 1).capacity;

                    //roomCapacity=examVector.get(i).room.capacity;
                    if (examCapacity > roomCapacity) {
                        roomOccupancyViolation++;
                    }
                }
            }
        }//System.out.println("roomOccupancyViolation="+roomOccupancyViolation);

        //Timeslot Utilisation
        int timeslotUtilisationViolation = 0;
        int timeslotDuration = 0;
        int examDuration = 0;
        for (int i = 0; i < solution.getNumberOfVariables(); i++) {
            ArrayList<Integer> exam = solution.getVariable(i);

            for (int j = 0; j < exam.size(); j++) {
                if (exam.get(j) == 0) {
                    continue;
                }
                timeslotDuration = timeslotVector.get(j).duration;
                examDuration = examVector.get(i).examDuration;
                if (timeslotDuration < examDuration) {
                    timeslotUtilisationViolation++;
                }
            }
        }//System.out.println("timeslotUtilisationViolation="+timeslotUtilisationViolation);

        //PeriodOrdering
        int periodOrderingViolation = 0;
        for (int i = 0; i < solution.getNumberOfVariables(); i++) {
            ArrayList<Integer> x = solution.getVariable(i);
            int k = getTimeslot(x);
            for (int j = 0; j < solution.getNumberOfVariables(); j++) {
                ArrayList<Integer> y = solution.getVariable(i);
                int l = getTimeslot(y);
//                for (k = 0; k < solution.getVariable(i).size(); k++) {
//                    if (solution.getVariable(i).get(k) != 0) {
//                        break;
//                    }
//                }
//                for (l = 0; l < solution.getVariable(i).size(); l++) {
//                    if (solution.getVariable(j)
//                            .get(l) != 0) {
//                        break;
//                    }
//                }

                if (exclusionMatrix[i][j] != 0) {
                    if (k == l) {
                        periodOrderingViolation++;
                    }
                }
                if (afterMatrix[i][j] != 0) {
                    if (k < l) {
                        periodOrderingViolation++;
                    }
                }
                if (coincidenceMatrix[i][j] != 0) {
                    if (conflictMatrix[i][j] != 0) {
                        break;
                    }
                    if (k != l) {
                        periodOrderingViolation++;
                    }
                }
            }
        }//System.out.println("periodOrderingViolation="+periodOrderingViolation);

        //RoomConstraints
        int roomConstraintViolation = 0;
        for (int i = 0; i < exclusiveExamsVector.size(); i++) {
            int exclusiveExam = exclusiveExamsVector.get(i).examId;
            int k, exclusiveRoom = -1;
            for (k = 0; k < solution.getVariable(exclusiveExam).size(); k++) {
                if (solution.getVariable(exclusiveExam).get(k) != 0) {
                    exclusiveRoom = solution.getVariable(exclusiveExam).get(k);
                    break;
                }
            }

            for (int j = 0; j < solution.getNumberOfVariables(); j++) {
                if (j == exclusiveExam) {
                    continue;
                }
                int r, room = -1;
                for (r = 0; r < solution.getVariable(j).size(); r++) {
                    if (solution.getVariable(j).get(r) != 0) {
                        room = solution.getVariable(j).get(r);
                        break;
                    }
                }
                if (exclusiveRoom == room) {
                    roomConstraintViolation++;
                }
            }
        }//System.out.println("roomConstraintViolation="+roomConstraintViolation);

        solution.setAttribute("CONFLICTS", conflicts);
        solution.setAttribute("ROOM_UTILIZATION_PENALTY", roomOccupancyViolation);
        solution.setAttribute("TIMESLOT_PENALTY", timeslotUtilisationViolation);
        solution.setAttribute("TIMESLOT_ORDERING_PENALTY", periodOrderingViolation);
        solution.setAttribute("ROOM_CONSTRAINT_PENALTY", roomConstraintViolation);
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
