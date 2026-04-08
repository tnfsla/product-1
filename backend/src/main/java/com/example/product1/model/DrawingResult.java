package com.example.product1.model;

import com.google.cloud.firestore.annotation.DocumentId;

  public class DrawingResult {
      private String pdfName;
      private String ocrText;
      private int    confidence;

      // 기존
      private String dwgNo;
      private String drawingName;
      private String partDesc;
      private String reviewer;

      // 신규 추가
      private String company;
      private String writer;
      private String designer;
      private String drafter;
      private String approver;

      @DocumentId
      private String id;
      private long savedAt;
      private String savedBy;

  

      // --- getters / setters ---
      public String getId()         { return id; }
      public void setId(String v)   { this.id = v; }
      public long getSavedAt()      { return savedAt; }
      public void setSavedAt(long v){ this.savedAt = v; }
      public String getPdfName()   { return pdfName; }
      public void setPdfName(String v)   { this.pdfName = v; }
      public String getOcrText()   { return ocrText; }
      public void setOcrText(String v)   { this.ocrText = v; }
      public int getConfidence()   { return confidence; }
      public void setConfidence(int v)   { this.confidence = v; }
      public String getDwgNo()        { return dwgNo; }
      public void setDwgNo(String v)        { this.dwgNo = v; }
      public String getDrawingName()  { return drawingName; }
      public void setDrawingName(String v)  { this.drawingName = v; }
      public String getPartDesc()  { return partDesc; }
      public void setPartDesc(String v)  { this.partDesc = v; }
      public String getReviewer()  { return reviewer; }
      public void setReviewer(String v)  { this.reviewer = v; }
      public String getCompany()   { return company; }
      public void setCompany(String v)   { this.company = v; }
      public String getWriter()    { return writer; }
      public void setWriter(String v)    { this.writer = v; }
      public String getDesigner()  { return designer; }
      public void setDesigner(String v)  { this.designer = v; }
      public String getDrafter()   { return drafter; }
      public void setDrafter(String v)   { this.drafter = v; }
      public String getApprover()  { return approver; }
      public void setApprover(String v)  { this.approver = v; }
      public String getSavedBy()   { return savedBy; }
      public void setSavedBy(String v)   { this.savedBy = v; }
  }