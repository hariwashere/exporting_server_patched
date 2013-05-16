package com.highcharts.export.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;


public class PatientDetailWriter {
    private PatientDetails patientDetails;
    private  String file;
    private String imagePainFileName;
    private String imageNauseaFileName;
    private String imageFatigueFileName;
    private String imageSleepFileName;
    private String imageConstipationFileName;

    public PatientDetailWriter() {
        file = UUID.randomUUID().toString().concat(".pdf");
    }

    public PatientDetailWriter(PatientDetails patientName, String uuid) {
        this();
        this.patientDetails = patientName;
        imagePainFileName = uuid.concat("_pain.png");
        imageNauseaFileName = uuid.concat("_nausea.png");
        imageFatigueFileName = uuid.concat("_fatigue.png");
        imageSleepFileName = uuid.concat("_sleep.png");
        imageConstipationFileName = uuid.concat("_constipation.png");
    }

    public String write() {
        try {
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();
            addMetaData(document);
            addContent(document);
            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }

    // iText allows to add metadata to the PDF which can be viewed in your Adobe
    // Reader
    // under File -> Properties
    private void addMetaData(Document document) {
        document.addTitle("Patient report");
        document.addSubject("Symptoms summary of the patient " + patientDetails.getPatientName());
        document.addAuthor("ET");
        document.addCreator("ET");
    }

    private void addContent(Document document) throws DocumentException, IOException {
        Paragraph name = new Paragraph("Patient name: " + patientDetails.getPatientName());
        document.add(name);
        addEmptyLine(name, 1);
        Paragraph dob = new Paragraph("Date of Birth: " + patientDetails.getDob());
        document.add(dob);
        addEmptyLine(dob, 1);
        Paragraph city = new Paragraph("City: " + patientDetails.getCity());
        document.add(city);
        addEmptyLine(city, 1);
        Paragraph state = new Paragraph("State: " + patientDetails.getState());
        document.add(state);
        addEmptyLine(state, 2);
        Image imagePain = Image.getInstance(imagePainFileName);
        imagePain.scalePercent(22f);
        document.add(imagePain);
        addEmptyLine(state, 2);
        Image imageNausea = Image.getInstance(imageNauseaFileName);
        imageNausea.scalePercent(22f);
        document.add(imageNausea);
        addEmptyLine(state, 2);
        Image imageSleep = Image.getInstance(imageSleepFileName);
        imageSleep.scalePercent(22f);
        document.add(imageSleep);
        addEmptyLine(state, 2);
        Image imageFatigue = Image.getInstance(imageFatigueFileName);
        imageFatigue.scalePercent(22f);
        document.add(imageFatigue);
        addEmptyLine(state, 2);
        Image imageConstipation = Image.getInstance(imageConstipationFileName);
        imageConstipation.scalePercent(22f);
        document.add(imageConstipation);
    }

    private static void addEmptyLine(Paragraph paragraph, int number) {
        for (int i = 0; i < number; i++) {
            paragraph.add(new Paragraph(" "));
        }
    }
}

