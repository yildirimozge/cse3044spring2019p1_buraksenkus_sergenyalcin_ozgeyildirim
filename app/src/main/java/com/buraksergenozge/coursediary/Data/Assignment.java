package com.buraksergenozge.coursediary.Data;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import com.buraksergenozge.coursediary.Fragments.AssignmentFragment;
import com.buraksergenozge.coursediary.Fragments.CourseFeed;
import com.buraksergenozge.coursediary.Fragments.CourseFragment;
import com.buraksergenozge.coursediary.Fragments.CreationDialog.AssignmentCreationDialog;
import com.buraksergenozge.coursediary.Fragments.CreationDialog.CourseCreationDialog;
import com.buraksergenozge.coursediary.Fragments.CreationDialog.CreationDialog;
import com.buraksergenozge.coursediary.Fragments.CreationDialog.NoteCreationDialog;
import com.buraksergenozge.coursediary.Fragments.CreationDialog.PhotoCreationDialog;
import com.buraksergenozge.coursediary.Fragments.SettingsFragment;
import com.buraksergenozge.coursediary.R;
import com.buraksergenozge.coursediary.Tools.ListAdapter;
import com.buraksergenozge.coursediary.Tools.StringManager;

import java.util.Calendar;

import static android.arch.persistence.room.ForeignKey.CASCADE;

@Entity(foreignKeys = @ForeignKey(entity = Course.class, parentColumns = "courseID", childColumns = "course", onDelete = CASCADE),
        indices = {@Index("course")})
public class Assignment extends AppContent implements Comparable<Assignment> {
    @PrimaryKey(autoGenerate = true)
    private long assignmentID;
    @ColumnInfo
    private String title;
    @ColumnInfo
    private Course course;
    @ColumnInfo
    private Calendar deadline;
    @Ignore
    private static final String[] relatedFragmentTags = {AssignmentFragment.tag, CourseFragment.tag, CourseFeed.tag};

    public Assignment(String title, Course course, Calendar deadline) {
        this.title = title;
        this.course = course;
        this.deadline = deadline;
    }

    public long getAssignmentID() {
        return assignmentID;
    }

    public void setAssignmentID(long assignmentID) {
        this.assignmentID = assignmentID;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public Calendar getDeadline() {
        return deadline;
    }

    public void setDeadline(Calendar deadline) {
        this.deadline = deadline;
    }

    public static CreationDialog getCreationDialog(int mode) {
        CreationDialog creationDialog = new AssignmentCreationDialog();
        creationDialog.mode = mode;
        return creationDialog;
    }

    @Override
    public void edit(AppCompatActivity activity) {
        AppContent.openCreationDialog(activity, getCreationDialog(CreationDialog.EDIT_MODE));
    }

    @Override
    public int getSaveMessage() {
        return R.string.assignment_saved;
    }

    @Override
    public void addOperation(AppCompatActivity activity) {
        this.assignmentID = CourseDiaryDB.getDBInstance(activity).assignmentDAO().addAssignment(this);
        course.getAssignments().add(this);
        updateNotificationTime(activity, false);
    }

    @Override
    public int getDeleteMessage() {
        return R.string.assignment_deleted;
    }

    @Override
    public void deleteOperation(AppCompatActivity activity) {
        course.getAssignments().remove(this);
        CourseDiaryDB.getDBInstance(activity).assignmentDAO().deleteAssignment(this);
        updateNotificationTime(activity, true);
    }

    @Override
    public void updateOperation(AppCompatActivity activity) {
        CourseDiaryDB.getDBInstance(activity).assignmentDAO().update(this);
        updateNotificationTime(activity, false);
    }

    @Override
    public void showInfo(AppCompatActivity activity) {
        AppContent.openCreationDialog(activity, getCreationDialog(CreationDialog.INFO_MODE));
    }

    @Override
    public void fillSpinners(CreationDialog creationDialog) {
        boolean isChanged = creationDialog.selectSemesterOnSpinner(course.getSemester());
        creationDialog.selectedSemester = (Semester)creationDialog.semesterSelectionSpinner.getSelectedItem();
        if (creationDialog instanceof CourseCreationDialog)
            return;
        if (isChanged || creationDialog.courseSelectionSpinner.getAdapter() == null) {
            ListAdapter<Course> adapter2 = new ListAdapter<>(creationDialog.getActivity(), creationDialog.selectedSemester.getCourses(), R.layout.list_item);
            creationDialog.courseSelectionSpinner.setAdapter(adapter2);
        }
        creationDialog.selectCourseOnSpinner(course);
        if (creationDialog instanceof NoteCreationDialog || creationDialog instanceof PhotoCreationDialog)
            return;
        creationDialog.selectedCourse = (Course) creationDialog.courseSelectionSpinner.getSelectedItem();
    }

    public void updateNotificationTime(AppCompatActivity activity, boolean cancel) {
        Calendar notificationTime = (Calendar) deadline.clone();
        SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        int assignmentRemindValue = sharedPref.getInt(SettingsFragment.ASSIGNMENT_REMIND_VALUE, 1);
        int assignmentRemindTimeType = sharedPref.getInt(SettingsFragment.ASSIGNMENT_REMIND_TIME_TYPE, 0);
        if (assignmentRemindTimeType == 0) // Hour
            notificationTime.add(Calendar.HOUR_OF_DAY, -1 * assignmentRemindValue); // Reminds "assignmentRemindValue" hours before
        else // Day
            notificationTime.add(Calendar.DATE, -1 * assignmentRemindValue); // Reminds "assignmentRemindValue" days before
        String notificationTitle = activity.getString(R.string.notification_assignment_title);
        String timeRepr = StringManager.getTimeRepresentation(AppContent.getTimeDifferenceInMillis(notificationTime, deadline), activity.getResources());
        String notificationMessage = activity.getResources().getString(R.string.notification_assignment_message, title, timeRepr);
        setOrCancelNotification(activity, (int)assignmentID, notificationTime, notificationTitle, notificationMessage, cancel);
    }

    @Override
    public String[] getRelatedFragmentTags() {
        return relatedFragmentTags;
    }

    @Override
    @NonNull
    public String toString() {
        return title;
    }

    @Override
    public int compareTo(Assignment assignment) {
        return (int)(deadline.getTimeInMillis() - assignment.deadline.getTimeInMillis());
    }
}