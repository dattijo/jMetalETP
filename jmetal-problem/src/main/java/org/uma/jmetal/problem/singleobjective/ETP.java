package org.uma.jmetal.problem.singleobjective;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
public class ETP extends AbstractIntegerMatrixProblem {

    NumberFormat formatter = new DecimalFormat("#0000.0000");

    Map<Integer, Student> studentMap;
    ArrayList<Exam> examVector;
    ArrayList<Exam> exclusiveExamsVector;
    ArrayList<TimeSlot> timeslotVector;
    ArrayList<Room> roomVector;
    ArrayList<Department> departmentVector;
    ArrayList<Faculty> facultyVector;
    ArrayList<Campus> campusVector;
    ArrayList<ArrayList<Integer>> timetableSolution;
    ArrayList<Integer> largestExams = new ArrayList<>();

    int numberOfExams;
    int numberOfTimeSlots;
    int numberOfCampuses;
    int numberOfFaculties;
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

    Graph<Integer, DefaultEdge> exGraph;
    Coloring coloredGraph;

    class Student {

        int sId, department;
        ArrayList<Exam> examList = new ArrayList<>();

        Student(int id) {
            sId = id;
        }                

        void addExam(Exam e) {
            examList.add(e);
        }
        
        void setdepartment(int d){
            department = d;
        }
    }

    class Exam {

        int examId, examDuration, studentsCount = 0;
        TimeSlot timeslot;
        Room room;
        ArrayList<Student> enrollmentList = new ArrayList<>();
        boolean exlcusive = false;

        Exam(int id, int duration) {
            examId = id;
            examDuration = duration;
        }

        void addStudent(Student student) {
            enrollmentList.add(student);
            studentsCount++;
        }

        void setTimeSlot(TimeSlot tSlot) {
            timeslot = tSlot;
        }

        void setRoom(Room rm) {
            room = rm;
        }
    }

    class Room {

        int capacity, roomId, distToFaculty, penalty;
        Faculty myFaculty;
        List<Exam> examList = new ArrayList<>();

        Room(int cap, int rId, int fId, int dToF, int pen) {
            capacity = cap;
            roomId = rId;
            myFaculty = facultyVector.get(fId - 1);
            distToFaculty = dToF;
            penalty = pen;
        }

        void allocateExam(Exam exam) {
            examList.add(exam);
        }

        Faculty getFaculty() {
            return myFaculty;
        }
    }
    
    class Department{
        int deptId;
        double longitude,latitude;
        List<Student> students = new ArrayList();
        
        Department(int dId, int lon, int lat){
            deptId = dId;
            longitude = lon;
            latitude  = lat;
        }
        
        void addStudent(Student std){
            students.add(std);
        }
        
    }

    class Faculty {

        int facId, distToCampus;
        double longitude, latitude;
        Campus myCampus;

        Faculty(int fId, int cId, double lon, double lat, int dToC) {
            myCampus = campusVector.get(cId - 1);
            facId = fId;
            longitude = lon;
            latitude = lat;
            distToCampus = dToC;
        }

        Campus getCampus() {
            return myCampus;
        }
    }

    class Campus {

        int campId;
        double longitude, latitude;

        Campus(int cId, double lon, double lat) {
            campId = cId;
            longitude = lon;
            latitude = lat;
        }
    }

    class TimeSlot {

        int id, day, pos, duration, penalty;
        ArrayList<Exam> examList = new ArrayList<>();

        TimeSlot(int i, int d, int t, int dur, int pen) {
            id = i;
            day = d;
            pos = t;
            duration = dur;
            penalty = pen;
        }

        void addExam(Exam e) {
            examList.add(e);
        }
    }

    public ETP(String problemFile) throws IOException {
        studentMap = new HashMap<>();
        examVector = new ArrayList<>();
        exclusiveExamsVector = new ArrayList<>();
        timeslotVector = new ArrayList();
        roomVector = new ArrayList();
        facultyVector = new ArrayList();
        campusVector = new ArrayList();
        timetableSolution = new ArrayList<ArrayList<Integer>>();
        spreadGap = 0;

        conflictMatrix = readProblem(problemFile);

        roomToRoomDistanceMatrix = new double[numberOfRooms][numberOfRooms];

        generateDistanceMatrices();

        exGraph = new SimpleGraph<>(DefaultEdge.class);
        createGraph(conflictMatrix);

        setNumberOfVariables(numberOfExams);
        setNumberOfObjectives(1);
//        this.setNumberOfConstraints(5);
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

        readTimeslots(token, found);
        readCampuses(token, found);
        readFaculties(token, found);
        readRooms(token, found);
        readTimeslotConstraints(token, found);
        readRoomConstraints(token, found);
        readWeightings(token, found);

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
        tok.nextToken();
        tok.nextToken();
        tok.nextToken();

        addExam(tok);

        //Read Enrollments
        fnd = false;
        int t = 0;
        while (!fnd) {
            if ((tok.sval != null) && ((tok.sval.compareTo("Periods") == 0))) {
                tok.nextToken();
                tok.nextToken();
                numberOfTimeSlots = (int) tok.nval;
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
                        
                        studentMap.get(currentStudent).examList.add(examVector.get(tok.lineno()-2));
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
        conflictMatrix = new int[numberOfExams][numberOfExams];

        //Generate Conflict Matrix
        ArrayList<Student> cleared = new ArrayList();
        for (int currExam = 0; currExam <= examVector.size() - 2; currExam++) {
            cleared.clear();
            int studentCount = examVector.get(currExam).enrollmentList.size();
            for (int currStudent = 1; currStudent <= studentCount; currStudent++) {
                Student student = examVector.get(currExam).enrollmentList.get(currStudent - 1);
                if (cleared.contains(student)) {
                    continue;
                }

                cleared.add(student);

                for (int nextExam = currExam + 1; nextExam <= examVector.size() - 1; nextExam++) {
                    if (examVector.get(nextExam).enrollmentList.contains(student)) {
                        int conflictCount = conflictMatrix[currExam][nextExam];
                        conflictCount++;
                        conflictMatrix[currExam][nextExam] = conflictCount;
                        conflictMatrix[nextExam][currExam] = conflictCount;
                    }
                }
            }
        }

        //int Matrix ConflictMatrix
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
        for (int v1 = 1; v1 <= numberOfExams; v1++) {
            exGraph.addVertex(v1);
        }

        for (int v1 = 1; v1 <= numberOfExams; v1++) {
            for (int v2 = 1; v2 <= numberOfExams; v2++) {
                if (cMat[v1 - 1][v2 - 1] != 0) {
                    exGraph.addEdge(v1, v2);
                }
            }
        }

    }

    void readTimeslots(StreamTokenizer tok, boolean fnd) throws IOException {
        //Read TimeSlots
//        System.out.println("Number of TimeSlots = "+numberOfTimeSlots);
        fnd = false;
        int t, pCount = 0;
        int day, pos, duration, penalty;
        day = pos = duration = 0;
        while (!fnd) {
            if ((tok.sval != null) && ((tok.sval.compareTo("Campuses") == 0))) {
                tok.nextToken();
                tok.nextToken();
                numberOfCampuses = (int) tok.nval;
//                System.out.println("Finished Reading TimeSlots.");
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
                        pos = (int) tok.nval;
                        tok.nextToken();
                        tok.nextToken();
                        duration = (int) tok.nval;
                        tok.nextToken();
                        tok.nextToken();
                        penalty = (int) tok.nval;
                        tok.nextToken();
                        addTimeSlot(++pCount, day, pos, duration, penalty);
                        break;
                }
            }
        }

//        System.out.println("Timeslots Vector");
//        for(int i=0; i<timeslotVector.size();i++)
//        {
//            System.out.print("Timeslot "+timeslotVector.get(i).id);
//            System.out.print(" in day "+timeslotVector.get(i).day);
//            System.out.print(" at position "+timeslotVector.get(i).pos);
//            System.out.println(" has "+timeslotVector.get(i).duration+" minutes");
//        }
    }

    void readCampuses(StreamTokenizer tok, boolean fnd) throws IOException {
        //Read Campuses
//        System.out.println("Number of Campuses = "+numberOfCampuses);
        int t = 0, cCount = 0;
        fnd = false;
        double lon = 0.0, lat = 0.0;
        while (!fnd) {
            if ((tok.sval != null) && ((tok.sval.compareTo("Faculties") == 0))) {
                tok.nextToken();
                tok.nextToken();
                numberOfFaculties = (int) tok.nval;
//                System.out.println("Finished Reading Campuses.");
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
//                        System.out.println("Long: "+lon+"\nLat: "+lat);
                        addCampus(++cCount, lon, lat);
                        break;
                }
            }
        }
    }

    void readFaculties(StreamTokenizer tok, boolean fnd) throws IOException {
        //Read Faculties
//        System.out.println("Number of Faculties = "+numberOfFaculties);
        fnd = false;
        int t, camp = 0, dToCamp = 0, fCount = 0;
        double lon = 0.0, lat = 0.0;
        while (!fnd) {
            if ((tok.sval != null) && ((tok.sval.compareTo("Rooms") == 0))) {
                tok.nextToken();
                tok.nextToken();

                numberOfRooms = (int) tok.nval;
//                System.out.println("Finished Reading Facuties.");
                fnd = true;
            } else {
                t = tok.nextToken();
                switch (t) {
                    case StreamTokenizer.TT_EOL:
                        break;
                    case StreamTokenizer.TT_NUMBER:
                        camp = (int) tok.nval;
                        tok.nextToken();
                        tok.nextToken();
                        dToCamp = (int) tok.nval;
                        tok.nextToken();
                        tok.nextToken();
                        lon = tok.nval;
                        tok.nextToken();
                        tok.nextToken();
                        lat = tok.nval;
                        tok.nextToken();
                        addFaculty(++fCount, camp, lon, lat, dToCamp);
                        break;
                }
            }
        }

//        System.out.println("Faculties Vector");
//        for(int i=0; i<facultyVector.size();i++)
//        {
//            System.out.print("Faculty "+facultyVector.get(i).facId);
//            System.out.print(" in campus "+facultyVector.get(i).campId+" is ");
//            System.out.print(" at longitude "+facultyVector.get(i).longitude);
//            System.out.print(" and latitude "+facultyVector.get(i).latitude+" is ");
//            System.out.println(facultyVector.get(i).distToCampus+"m from main entrance.");         
//        }
    }

    void readRooms(StreamTokenizer tok, boolean fnd) throws IOException {
        //Read Rooms
//        System.out.println("Number of Rooms = "+numberOfRooms);
        fnd = false;
        int t, rCount = 0, cap, fac, dToFac, penalty;
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
                        fac = (int) tok.nval;
                        tok.nextToken();
                        tok.nextToken();
                        dToFac = (int) tok.nval;
                        tok.nextToken();
                        tok.nextToken();
                        penalty = (int) tok.nval;
                        tok.nextToken();
                        addRoom(cap, ++rCount, fac, dToFac, penalty);
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
//        System.out.println("twoinarow:"+twoInARow);
//        System.out.println("twoinaday:"+twoInADay);
//        System.out.println("periodSpread:"+periodSpread);
//        System.out.println("nonMixedDurations:"+nonMixedDurations);
//        System.out.println("numberOfLargestExams:"+numberOfLargestExams);
//        System.out.println("numberOfLastPeriods:"+numberOfLastPeriods);
//        System.out.println("frontLoadPenalty:"+frontLoadPenalty);
    }

    void generateDistanceMatrices() {
        for (int i = 0; i < numberOfRooms; i++) {
            for (int j = i; j < numberOfRooms; j++) {
                double long1, long2, lat1, lat2;
                Room rm1 = roomVector.get(i);
                Room rm2 = roomVector.get(j);

                if (rm1.getFaculty().facId == rm2.getFaculty().facId) {
                    roomToRoomDistanceMatrix[i][j] = 0.0;
                    roomToRoomDistanceMatrix[j][i] = 0.0;
                } else {
                    if (rm1.getFaculty().getCampus().campId == rm2.getFaculty().getCampus().campId) {
                        long1 = rm1.getFaculty().longitude;
                        lat1 = rm1.getFaculty().latitude;
                        long2 = rm2.getFaculty().longitude;
                        lat2 = rm2.getFaculty().latitude;

                        roomToRoomDistanceMatrix[i][j] = rm1.distToFaculty
                                + gpsDistance(long1, lat1, long2, lat2) + rm2.distToFaculty;
                        roomToRoomDistanceMatrix[j][i] = rm1.distToFaculty
                                + gpsDistance(long1, lat1, long2, lat2) + rm2.distToFaculty;
                    } else {
                        long1 = rm1.getFaculty().getCampus().longitude;
                        lat1 = rm1.getFaculty().getCampus().latitude;
                        long2 = rm2.getFaculty().getCampus().longitude;
                        lat2 = rm2.getFaculty().getCampus().latitude;

                        //rm2Fac+Fac2Cam+Cam2Cam+Cam2Fac+Fac2rm
                        roomToRoomDistanceMatrix[i][j] = rm1.distToFaculty + rm1.getFaculty().distToCampus
                                + gpsDistance(long1, lat1, long2, lat2) + rm2.getFaculty().distToCampus
                                + rm2.distToFaculty;
                        roomToRoomDistanceMatrix[j][i] = rm1.distToFaculty + rm1.getFaculty().distToCampus
                                + gpsDistance(long1, lat1, long2, lat2) + rm2.getFaculty().distToCampus
                                + rm2.distToFaculty;
                    }
                }
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
        int line = tok.lineno() - 1;
        examDuration = (int) tok.nval;
        if (line <= numberOfExams) {
            examVector.add(new Exam(line, examDuration));
        }
    }

    void addTimeSlot(int id, int d, int p, int dur, int pen) {
        timeslotVector.add(new TimeSlot(id, d, p, dur, pen));
    }

    void addFaculty(int id, int c, double lon, double lat, int dToC) {
        facultyVector.add(new Faculty(id, c, lon, lat, dToC));
    }

    void addRoom(int c, int id, int f, int dToF, int pen) {
        roomVector.add(new Room(c, id, f, dToF, pen));
//        overallRoomCapacity+=c;
    }

    void addCampus(int id, double lon, double lat) {
        campusVector.add(new Campus(id, lon, lat));
    }

    void allocateTimeSlots() {
        ArrayList<TimeSlot> availableTimeSlots = new ArrayList<>();
        availableTimeSlots.addAll(timeslotVector);
        for (int t = 0; t < availableTimeSlots.size(); t++) {
            availableTimeSlots.get(t).examList.clear();
        }
//          coloredGraph  = new LargestDegreeFirstColoring(exGraph).getColoring();
//        coloredGraph  = new GreedyColoring(exGraph).getColoring();
//        coloredGraph  = new BrownBacktrackColoring(exGraph).getColoring();
//        coloredGraph = new RandomGreedyColoring(exGraph).getColoring();
        coloredGraph = new SaturationDegreeColoring(exGraph).getColoring();
//        coloredGraph = new ColorRefinementAlgorithm(exGraph, new GreedyColoring(exGraph).getColoring()).getColoring();

        for (int i = 0; i < examVector.size(); i++) {
            int allocatedTime = (int) coloredGraph.getColors().get(i + 1);
            examVector.get(i).setTimeSlot(
                    availableTimeSlots.get(allocatedTime));
//            System.out.println("Exam "+examVector.get(i).examId+" @ Timeslot "+examVector.get(i).timeslot.id);            
            availableTimeSlots.get(allocatedTime).addExam(examVector.get(i));
        }

//        for(int t=0;t<availableTimeSlots.size();t++)
//        {
//            System.out.println("Timeslot "+availableTimeSlots.get(t).id+" has "+availableTimeSlots.get(t).examList.size()+" exams");
//        }
    }

    void allocateRooms(ArrayList rooms) {
        class ExamComparator implements Comparator<Exam> {

            @Override
            public int compare(Exam a, Exam b) {
                return a.studentsCount < b.studentsCount ? -1 : a.studentsCount == b.studentsCount ? 0 : 1;
            }
        }

        class RoomComparator implements Comparator<Room> {

            @Override
            public int compare(Room a, Room b) {
                return a.capacity < b.capacity ? -1 : a.capacity == b.capacity ? 0 : 1;
            }
        }

//        System.out.println("\n\nROOM ALLOCATION FOR NEW SOLUTION:");
        Map<Integer,ArrayList> freeTimeslotRoomMap = new HashMap();
//        ArrayList<Exam> allocatedExams = new ArrayList();
        ArrayList<Exam> unAllocatedExams = new ArrayList();
        ArrayList<Exam> mainUnAllocatedExams = new ArrayList();
        ArrayList<Room> tmpRoomVector = new ArrayList();
        
        tmpRoomVector.addAll(roomVector);
        Collections.sort(tmpRoomVector, new RoomComparator().reversed());
//        for(int r =0; r<tmpRoomVector.size();r++){
//            System.out.println("Room "+tmpRoomVector.get(r).roomId+". Capacity = "+tmpRoomVector.get(r).capacity);
//        } 
        
        for (int t = 0; t < timeslotVector.size(); t++) {
//            if (timeslotVector.get(t).examList.size() <= 0) {
//                continue;
//            }            
            tmpRoomVector.clear();
            tmpRoomVector.addAll(roomVector);
            Collections.sort(tmpRoomVector, new RoomComparator().reversed());
                       
            TimeSlot tmpT = timeslotVector.get(t);
//            System.out.println("\nNow in Timeslot "+tmpT.id+" having "+tmpT.examList.size()+" exams.");

            ArrayList<Exam> tmpExamVector = new ArrayList();
            tmpExamVector.addAll(tmpT.examList);
            Collections.sort(tmpExamVector, new ExamComparator().reversed());
//            for(int e =0; e<tmpExamVector.size();e++)
//            {
//                System.out.println("Exam "+tmpExamVector.get(e).examId+". Enrollment = "+tmpExamVector.get(e).studentsCount);                
//            } 

            int e = 0;
            while (e < tmpExamVector.size()) {                
//                System.out.println("Allocating rooms to "+tmpExamVector.size()+" exams...");
                int r = 0;

//                System.out.println("Now in Exam "+tmpExamVector.get(e).examId);
                while (tmpRoomVector.size() > 0 && r < tmpRoomVector.size() && (tmpExamVector.size() > 0) && e < tmpExamVector.size()) {
//                    System.out.println("\nSearching for room to exam "+tmpExamVector.get(0).examId);
                    Exam tmpE = tmpExamVector.get(e);
                    Room tmpR = tmpRoomVector.get(r);

                    if (tmpE.studentsCount <= tmpR.capacity) {
                        tmpE.setRoom(tmpR);
//                        allocatedExams.add(tmpE);//System.out.println("Exam "+tmpE.examId+" has been set to room "+tmpR.roomId);
                        examVector.get(tmpE.examId - 1).setRoom(tmpR);//System.out.println("Removing exam "+tmpE.examId);
                        tmpExamVector.remove(tmpE);//System.out.println("tmpExamVector now has "+tmpExamVector.size()+" exams");
//                                                  System.out.println("Removing room "+tmpR.roomId);
                        tmpRoomVector.remove(tmpR);//System.out.println("tmpRoomVector now has "+tmpRoomVector.size()+" rooms");
                    } else r++;                                                           
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
//                    System.out.print("...skipping exam");//+tmpExamVector.get(e).examId+"\n");// for exam "+tmpExamVector.get(0).examId+" with "+tmpExamVector.get(0).studentsCount);
                    unAllocatedExams.add(tmpExamVector.get(e));
//                    System.out.println("Allocated = "+allocatedExams.size());                    
//                    System.out.println("Unallocated = "+unAllocatedExams.size());
                    e++;
                    
                } 
                
            }
            
//            System.out.println("****free rooms ="+tmpRoomVector.size()); 
            ArrayList tmpFreeRooms = new ArrayList(tmpRoomVector);
            if(tmpRoomVector.size()>0){
                freeTimeslotRoomMap.put(t+1, tmpFreeRooms);
            }
//            System.out.println("\nfreeTimeslotRoomMap: ");
//            for(Map.Entry<Integer, ArrayList> entry : freeTimeslotRoomMap.entrySet()){
////                System.out.print("Timeslot "+timeslotVector.get(entry.getKey()-1).id+" Free Rooms:");
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
//                    System.out.print(unAllocatedExams.get(i).examId+", ");
////                    int studs = unAllocatedExams.get(i).studentsCount;
////                    System.out.print(": no. of studs = "+studs);
////                    currFreeStuds+=studs;
//                }
////                System.out.println("total unallocated studs = "+currFreeStuds);
//            }
            
            mainUnAllocatedExams.addAll(unAllocatedExams);
            unAllocatedExams.clear();
        }

        //RE-ALLOCATING UNALLOCATED EXAMS - version 2
//        System.out.println("RE-ALLOCATING UNALLOCATED EXAMS");
        int unAllocatedExamsCount=mainUnAllocatedExams.size();
        Collections.sort(mainUnAllocatedExams,new ExamComparator().reversed());
        for(int i=0; i<unAllocatedExamsCount;i++){  
            boolean cannotAllocateTime = false;  
            Exam exam1 = mainUnAllocatedExams.get(0);
//            System.out.println("Attempting to allocate exam..."+exam1.examId);
            ArrayList<Integer> freeTimeslots = new ArrayList(freeTimeslotRoomMap.keySet());
            if(freeTimeslots.size()>0){
                int timeslotIndex=0;
                int timeslot = freeTimeslots.get(timeslotIndex);
//                System.out.println("\tTrying Timeslot.."+timeslot); 
                boolean conflictFound=false;
                
                for(int j = 0;j<examVector.size();j++){                
                    Exam exam2 = examVector.get(j);
                    if(timeslot!=exam2.timeslot.id)continue; 
                    int exam1ID = exam1.examId-1;
                    int exam2ID = exam2.examId-1;
                    int conf = conflictMatrix[exam1ID][exam2ID];
                    if(conf!=0){
//                        System.out.println("\tExam "+(exam1ID+1)+" conflicts with "+" Exam "+(exam2ID+1)
//                                +" @ slots "+timeslot+" & "+exam2.timeslot.id+" resp.\t");
//                        System.out.println("\tconflictMatrix["+(exam1ID+1)+"]["+(exam2ID+1)+"]= "+conf);
                        conflictFound=true;
                        break;
                    }                                      
                }
                boolean roomAllocated=false;
                boolean noRoom=false;
                while(true){
                    if(roomAllocated||cannotAllocateTime)break;          
                    while(conflictFound||noRoom){
                        noRoom=false;
                        timeslotIndex++;
                        
                        if(timeslotIndex>=freeTimeslots.size()){
//                            System.out.println("\tTimeslots Exhausted. Cannot allocate exam"+exam1.examId
//                                    +". Moving to next exam");
                            cannotAllocateTime = true;
                            break;
                        }
                        else{
                            timeslot = freeTimeslots.get(timeslotIndex);
//                            System.out.println("\tTrying next Timeslot = "+timeslot);
                            conflictFound=false;
                            for(int j = 0;j<examVector.size();j++){                
                                Exam exam2 = examVector.get(j);
                                if(timeslot!=exam2.timeslot.id)continue; 
                                int exam1ID = exam1.examId-1;
                                int exam2ID = exam2.examId-1;
                                int conf = conflictMatrix[exam1ID][exam2ID];
                                if(conf!=0){
//                                    System.out.println("\tExam "+(exam1ID+1)+" conflicts with "+" Exam "
//                                            +(exam2ID+1)+" @ slots "+timeslot+" & "+exam2.timeslot.id+" resp.\t");
//                                    System.out.println("\tconflictMatrix["+(exam1ID+1)+"]["+(exam2ID+1)+"]= "+conf);
                                    conflictFound=true;
                                    break;
                                }                            
                            }
                        }
                    }
//                    System.out.println("\tNo conflict found with all exams on timeslot "+timeslot);              
                    ArrayList<Room> freeRooms = freeTimeslotRoomMap.get(timeslot);
                    Collections.sort(freeRooms, new RoomComparator().reversed());
                    roomAllocated =false;
                    int roomIndex=0;
                    while(!roomAllocated){                        
                        if(roomIndex>=freeRooms.size()){
//                            System.out.println("\tRooms exhausted. Trying next timeslot...");
                            noRoom=true;
                            break;
                        }    
                        
                        int selectedRoom = freeRooms.get(roomIndex).roomId-1;
                        Room rm = roomVector.get(selectedRoom);
                        if(exam1.studentsCount<=rm.capacity){
//                            System.out.println("\tRoom "+rm.roomId+" is suitable.");
                            examVector.get(exam1.examId-1).setTimeSlot(timeslotVector.get(timeslot-1));
                            examVector.get(exam1.examId-1).setRoom(rm);
//                            System.out.println("\tExam "+exam1.examId+" has been set to room "
//                                    +exam1.room.roomId+" @ Timeslot "+exam1.timeslot.id);
                            //freeTimeslotRoomMap.get(timeslot).remove(randRoomIndex);
                            freeTimeslotRoomMap.get(timeslot).remove(0);
                            mainUnAllocatedExams.remove(exam1);
//                            System.out.println("\tRooms remaining: "+freeTimeslotRoomMap.get(timeslot).size());                            
                            if(freeTimeslotRoomMap.get(timeslot).size()<=0){
                                freeTimeslotRoomMap.remove(timeslot);
//                                System.out.println("\tNo more rooms in timeslot "+timeslot+". Timeslot removed");
//                                System.out.println("\tTimeslots remaining "+freeTimeslotRoomMap.size());
                            }
                            roomAllocated=true;                            
                        }
                        else{   
                            roomIndex++;
//                            System.out.println("\tRoom "+rm.roomId+" is not suitable. Trying again...");           
                        }
                    }
                }
            }
            else{
//                System.out.println("No free timeslot available....moving to next exam");
            }
        }        
        
//        System.out.println("\n****ROOM ALLOCATION SUMMARY*****");
//        for(int e =0; e<examVector.size();e++)
//        {
//            //Exam tmpE = (Exam)tmpExamVector.get(e);
//            if(!mainUnAllocatedExams.contains(examVector.get(e)))
//            {
//                System.out.println("Exam "+examVector.get(e).examId
//                    +" has been set to room "+examVector.get(e).room.roomId                                            
//                    +" in timeslot "+examVector.get(e).timeslot.id);
//            }            
//        }
//        System.out.println(mainUnAllocatedExams.size()+" exams cannot be allocated:");
//        Collections.sort(mainUnAllocatedExams, new ExamComparator().reversed());
//        for(int i=0;i<mainUnAllocatedExams.size();i++){
//            System.out.println((i+1)+" - Exam: "+mainUnAllocatedExams.get(i).examId+". Enrollment = "+mainUnAllocatedExams.get(i).studentsCount);
//        }System.out.println();
        
    }

    ArrayList<ArrayList<Integer>> generateTimeTableMatrix() {
        ArrayList<Integer> randRooms = new ArrayList<>();
        for (int i = 0; i < numberOfRooms; i++) {
            randRooms.add(i);
        }
        allocateTimeSlots();
        allocateRooms(randRooms);

        ArrayList<Integer> tmpSlots = new ArrayList<>();
        for (int i = 0; i < numberOfTimeSlots; i++) {
            tmpSlots.add(i, 0);
        }

        for (int j = 0; j < examVector.size(); j++) {
            timetableSolution.add(j, new ArrayList<Integer>(tmpSlots));
        }

        for (int i = 0; i < numberOfExams; i++) {
            for (int j = 0; j < numberOfTimeSlots; j++) {
                if (examVector.get(i).timeslot.id != j + 1) {
                    continue;
                }
                int room = -1;
                if (examVector.get(i).room != null) {
                    room = examVector.get(i).room.roomId;
                }

                timetableSolution.get(i).set(j, room);
            }
        }

        return timetableSolution;
    }

    @Override
    public IntegerMatrixSolution<ArrayList<Integer>> createSolution() {
        timetableSolution = generateTimeTableMatrix();

        DefaultIntegerMatrixSolution solution = new DefaultIntegerMatrixSolution(getListOfExamsPerVariable(), getNumberOfObjectives());
//        System.out.println("Creating solution...");
        for (int i = 0; i < getLength(); i++) {
            solution.setVariable(i, timetableSolution.get(i));
        }
//        System.out.println("new solution:"+solution.getVariables());
        return solution;
    }

    @Override
    public void evaluate(IntegerMatrixSolution<ArrayList<Integer>> solution) {
        double proximityFitness = evaluateProximityFitness(solution);
        double movementFitness = evaluateMovementFitness(solution);                 //movement cost
        double roomUtilizationFitness = evaluateRoomUtilizationFitness(solution);   //room cost
        
        double compositeFitness = proximityFitness+movementFitness+roomUtilizationFitness;
        int itc2007Fitness = evaluateITC2007Fitness(solution);

        this.evaluateConstraints(solution);
//        solution.setObjective(0, proximityFitness);
        solution.setObjective(0, movementFitness);        
//        solution.setObjective(0, roomUtilizationFitness); 
//        solution.setObjective(0, compositeFitness);
//        solution.setObjective(0, itc2007Fitness);

//        System.out.println("Objective =" + solution.getObjective(0)+" Conflicts = "+solution.getAttribute("CONFLICTS"));
    }

    public double evaluateProximityFitness(IntegerMatrixSolution<ArrayList<Integer>> solution) {
        //proximity constraint 
        double proximity = 0.0;
        for (int i = 0; i < solution.getNumberOfVariables(); i++) {
            int slot1 = getTimeslot(solution.getVariable(i));
            for (int j = 0; j < solution.getNumberOfVariables(); j++) {
                if (i == j) {
                    continue;
                }
                int slot2 = getTimeslot(solution.getVariable(j));

                if (conflictMatrix[i][j] != 0) {
                    int prox = (int) Math.pow(2, (5 - Math.abs(slot1 - slot2)));
                    proximity += prox * conflictMatrix[i][j];
                }
            }
        }
        return proximity/studentMap.size();
    }

    public double evaluateMovementFitness(IntegerMatrixSolution<ArrayList<Integer>> solution) {
        //movement constraint
        double movementCost = 0.0;
//        System.out.println("\n\nEVALUATING MOVEMENT COST for solution:");
//        System.out.println(solution.getVariables());
        for (Map.Entry<Integer, Student> currStudent : studentMap.entrySet()) {
//            System.out.println("Student="+currStudent.getKey());
            for (int e1 = 0; e1 < currStudent.getValue().examList.size(); e1++) {                
                Exam cExam = examVector.get(currStudent.getValue().examList.get(e1).examId - 1);
                
                for (int e2 = e1; e2 < currStudent.getValue().examList.size(); e2++) {
                    if (e1 == e2) {
                        continue;
                    }                    
                    Exam nExam = examVector.get(currStudent.getValue().examList.get(e2).examId - 1);
//                    if(cExam.room==null)continue;int rm1 = cExam.room.roomId;
//                    if(nExam.room==null)continue;int rm2 = nExam.room.roomId;
//                    
//                    fitness2+=roomToRoomDistanceMatrix[rm1-1][rm2-1];
                    int room1 = getRoom(solution.getVariable(cExam.examId - 1))-1;
                    int room2 = getRoom(solution.getVariable(nExam.examId - 1))-1;
                    if (room1 > 0 && room2 > 0) {
//                        System.out.println("room="+room1+"\nroom2="+room2);
                        double currCost= roomToRoomDistanceMatrix[room1][room2];
//                        System.out.println("Movement cost btw exam "+cExam.examId+" in room "+(room1+1)+" to exam "+nExam.examId
//                            +" in room "+(room2+1)+" is "+currCost);
                        movementCost += currCost;
                        
                        
                    }
                }
            }
        }
        return movementCost/studentMap.size();
    }
    


    public double evaluateRoomUtilizationFitness(IntegerMatrixSolution<ArrayList<Integer>> solution) {
        //room under-utilization and over-utilization constraint 
        int roomCap=0,studSize=0;
        double underUtilization = 0.0;
        double overUtilization = 0.0;        

        for (int i = 0; i < solution.getNumberOfVariables(); i++) {
            ArrayList<Integer> exam = solution.getVariable(i);                                      
            int room = getRoom(exam);
            if(room!=-1)roomCap = roomVector.get(room-1).capacity;
            studSize = examVector.get(i).studentsCount;
            if (room != -1) {       
                if(studSize<roomCap)underUtilization += ( roomCap - studSize);                                    
                else if(studSize>roomCap)overUtilization += (studSize - roomCap);                                    
            }
            else{               
                overUtilization+=studSize;
            }
        }
        return (underUtilization+overUtilization)/roomVector.size();
    }

    public int evaluateITC2007Fitness(IntegerMatrixSolution<ArrayList<Integer>> solution) {
        //TwoInARow,TwoInADay,PeriodSpread
        TimeSlot slot1, slot2;
        int twoInARowCount = 0;
        int twoInADayCount = 0;
        int periodSpreadCount = 0;
        for (Map.Entry<Integer, Student> currStudent : studentMap.entrySet()) //        for(int i=0; i < studentMap.size();i++)
        {
            for (int j = 0; j < currStudent.getValue().examList.size(); j++) {
                for (int k = 0; k < currStudent.getValue().examList.size(); k++) {
                    if (j == k) {
                        continue;
                    }
//                    System.out.print("-->"+getTimeslot(solution.getVariable(currStudent.getValue().examList.get(j).examId - 1))+" ");
                    int timeS1 = getTimeslot(solution.getVariable(currStudent.getValue().examList.get(j).examId - 1));
                    int timeS2 = getTimeslot(solution.getVariable(currStudent.getValue().examList.get(k).examId - 1));                    

                    slot1 = timeslotVector.get(timeS1);
                    slot2 = timeslotVector.get(timeS2);
                    if (slot1.day == slot2.day) {
                        if (Math.abs(slot1.pos - slot2.pos) == 1) {
                            twoInARowCount++;
                        } else if (Math.abs(slot1.pos - slot2.pos) > 1) {
                            twoInADayCount++;
                        }
                    }
                    if (Math.abs(slot1.pos - slot2.pos) < spreadGap) {
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
                    int room = getRoom(solution.getVariable(j)) - 1;
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
                largestExams.add(examVector.get(i).examId - 1);
            }
        }
        for (int i = 0; i < solution.getNumberOfVariables(); i++) {
            int timeslot = getTimeslot(solution.getVariable(i));
            if (largestExams.contains(i) && timeslot > (timeslotVector.size() - numberOfLastPeriods)) {
                frontLoadViolation++;
            }
        }
        int frontLoadPenalty = frontLoadViolation * frontLoadWeight;

        //ITC2007 Objective Function
        return twoInARowPenalty + twoInADayPenalty + periodSpreadPenalty
                + nonMixedDurationPenalty + frontLoadPenalty + timeslotPenalty + roomPenalty;
    }

    public void evaluateConstraints(IntegerMatrixSolution<ArrayList<Integer>> solution) {
        //conflicts
        int conflicts = 0;
        for (int i = 0; i < solution.getNumberOfVariables(); i++) {
            for (int j = 0; j < solution.getNumberOfVariables(); j++) {
                if (i == j) {
                    continue;
                }

                int slot1 = 0, slot2 = 0;

                ArrayList<Integer> x = solution.getVariable(i);
                while (x.get(slot1) == 0) {
                    slot1++;
                }

                ArrayList<Integer> y = solution.getVariable(j);
                while (y.get(slot2) == 0) {
                    slot2++;
                }

                if (conflictMatrix[i][j] != 0) {
                    if (slot1 == slot2) {
                        conflicts++;
                    }
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
            for (int j = 0; j < solution.getNumberOfVariables(); j++) {
                int k, l;
                for (k = 0; k < solution.getVariable(i).size(); k++) {
                    if (solution.getVariable(i).get(k) != 0) {
                        break;
                    }
                }
                for (l = 0; l < solution.getVariable(i).size(); l++) {
                    if (solution.getVariable(j).get(l) != 0) {
                        break;
                    }
                }

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
            int exclusiveExam = exclusiveExamsVector.get(i).examId - 1;
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
            if (exam.get(i) != 0) {
                return i;
            }
        }
        return -1;
    }

    public int getRoom(ArrayList<Integer> exam) {
        for (int i = 0; i < exam.size(); i++) {
            if (exam.get(i) != 0) {
                return exam.get(i);
            }
        }
        return -1;
    }    
    
    @Override
    public int[] getRoomCapacities(){
        int [] roomCapacities = new int[roomVector.size()];
        for(int i=0; i<roomVector.size();i++){
            roomCapacities[i]=roomVector.get(i).capacity;
        }
        return roomCapacities;
    }
    
    @Override
    public int [] getExamEnrollments(){
        int [] examEnrollments = new int[examVector.size()];
        for(int i=0; i<examVector.size();i++){
            examEnrollments[i]=examVector.get(i).studentsCount;
        }
        return examEnrollments;
    }
    
    @Override
    public int getNumberOfTimeslots(){
        return numberOfTimeSlots;
    }
    
    public ArrayList getLargestExams(){
        return largestExams;
    }
}
