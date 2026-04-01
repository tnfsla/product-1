  package com.example.product1.service;

  import com.example.product1.model.DrawingResult;
  import org.apache.pdfbox.Loader;
  import org.apache.pdfbox.pdmodel.PDDocument;
  import org.apache.pdfbox.pdmodel.PDPage;
  import org.apache.pdfbox.pdmodel.common.PDRectangle;
  import org.apache.pdfbox.rendering.PDFRenderer;
  import org.apache.pdfbox.text.PDFTextStripperByArea;
  import org.springframework.stereotype.Service;
  import org.springframework.web.multipart.MultipartFile;

  import javax.imageio.ImageIO;
  import java.awt.geom.Rectangle2D;
  import java.awt.image.BufferedImage;
  import java.io.*;
  import java.nio.charset.StandardCharsets;
  import java.nio.file.Files;
  import java.util.*;
  import java.util.regex.*;

  @Service
  public class OcrService {

      public DrawingResult processFile(MultipartFile file) throws Exception {
          File tempFile = File.createTempFile("upload_", ".pdf");
          try {
              file.transferTo(tempFile);
              return processPdf(tempFile, file.getOriginalFilename());
          } finally {
              tempFile.delete();
          }
      }

      // ─── 메인 파이프라인 ─────────────────────────────────────────

      private DrawingResult processPdf(File pdfFile, String originalName) throws Exception {
          // 1) PDFBox 텍스트 레이어 시도 (멀티-리전)
          String pdfText = extractPdfTextMultiRegion(pdfFile);

          String ocrText;
          boolean usedOcr = false;

          if (pdfText != null && pdfText.length() >= 10) {
              ocrText = pdfText;
          } else {
              // 2) FAST OCR — PSM 11, DPI 150
              ocrText = runTesseract(pdfFile, 11, 150);
              usedOcr = true;
          }

          DrawingResult result = parseFields(ocrText, originalName);
          result.setPdfName(originalName);
          result.setOcrText(ocrText);

          int conf = estimateConfidence(result);

          // 3) AUTO 재시도: 품명·검도자 미추출 또는 confidence < 60
          boolean needRetry = usedOcr
                  && (result.getPartDesc() == null || result.getReviewer() == null || conf < 60);
          if (needRetry) {
              String slowText = runTesseract(pdfFile, 2, 200);
              String merged   = mergeOcrTexts(ocrText, slowText);
              DrawingResult retryResult = parseFields(merged, originalName);
              int retryConf = estimateConfidence(retryResult);
              if (retryConf > conf) {
                  retryResult.setPdfName(originalName);
                  retryResult.setOcrText(merged);
                  retryResult.setConfidence(retryConf);
                  return retryResult;
              }
          }

          result.setConfidence(conf);
          return result;
      }

      // ─── PDF 텍스트 레이어 — 멀티-리전 ──────────────────────────

      private String extractPdfTextMultiRegion(File pdfFile) {
          try (PDDocument doc = Loader.loadPDF(pdfFile)) {
              PDPage page = doc.getPage(0);
              PDRectangle media = page.getMediaBox();
              float w = media.getWidth();
              float h = media.getHeight();

              // PDF 좌표계: 원점 = 좌하단. y=0 → 하단, y=h → 상단.
              // rectFromBottom(w, h, xFrac, yFrac, wFrac, hFrac):
              //   x = w*xFrac, y = h*yFrac, width = w*wFrac, height = h*hFrac
              Map<String, Rectangle2D> regions = new LinkedHashMap<>();
              regions.put("LR_45W_35H", rectFromBottom(w, h, 0.55f, 0.00f, 0.45f, 0.35f));
              regions.put("LR_35W_30H", rectFromBottom(w, h, 0.65f, 0.00f, 0.35f, 0.30f));
              regions.put("LR_40W_25H", rectFromBottom(w, h, 0.60f, 0.00f, 0.40f, 0.25f));
              regions.put("RIGHT_MID",  rectFromBottom(w, h, 0.65f, 0.25f, 0.35f, 0.35f));

              PDFTextStripperByArea stripper = new PDFTextStripperByArea();
              stripper.setSortByPosition(true);
              for (Map.Entry<String, Rectangle2D> e : regions.entrySet()) {
                  stripper.addRegion(e.getKey(), e.getValue());
              }
              stripper.extractRegions(page);

              // 가장 텍스트가 많은 리전 선택
              String best = "";
              for (String key : regions.keySet()) {
                  String t = stripper.getTextForRegion(key).trim();
                  if (t.length() > best.length()) best = t;
              }
              return best.isEmpty() ? null : best;
          } catch (Exception e) {
              return null;
          }
      }

      private static Rectangle2D rectFromBottom(float w, float h,
              float xFrac, float yFrac, float wFrac, float hFrac) {
          return new Rectangle2D.Float(w * xFrac, h * yFrac, w * wFrac, h * hFrac);
      }

      // ─── Tesseract OCR ───────────────────────────────────────────

      private String runTesseract(File pdfFile, int psm, int dpi) throws Exception {
          File imgFile = File.createTempFile("ocr_crop_", ".png");
          try (PDDocument doc = Loader.loadPDF(pdfFile)) {
              PDFRenderer renderer = new PDFRenderer(doc);
              BufferedImage full = renderer.renderImageWithDPI(0, dpi);

              int iw = full.getWidth(), ih = full.getHeight();
              // 우측하단 45% x 35% 크롭
              int cropX = (int)(iw * 0.55), cropY = (int)(ih * 0.60);
              BufferedImage cropped = full.getSubimage(cropX, cropY, iw - cropX, ih - cropY);
              ImageIO.write(binarize(cropped), "PNG", imgFile);
          }

          File outBase = File.createTempFile("tess_out_", "");
          try {
              ProcessBuilder pb = new ProcessBuilder(
                  "tesseract", imgFile.getAbsolutePath(),
                  outBase.getAbsolutePath(),
                  "-l", "kor+eng",
                  "--psm", String.valueOf(psm)
              );
              pb.redirectErrorStream(true);
              pb.start().waitFor();

              File txtFile = new File(outBase.getAbsolutePath() + ".txt");
              if (txtFile.exists()) {
                  String text = new String(Files.readAllBytes(txtFile.toPath()), StandardCharsets.UTF_8);
                  txtFile.delete();
                  return text.trim();
              }
              return "";
          } finally {
              imgFile.delete();
              outBase.delete();
          }
      }

      /** 이진화: OCR 인식률 향상 */
      private BufferedImage binarize(BufferedImage src) {
          int w = src.getWidth(), h = src.getHeight();
          BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
          for (int y = 0; y < h; y++) {
              for (int x = 0; x < w; x++) {
                  int rgb = src.getRGB(x, y);
                  int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
                  int gray = (r * 299 + g * 587 + b * 114) / 1000;
                  int bin  = gray < 160 ? 0 : 255;
                  out.setRGB(x, y, (bin << 16) | (bin << 8) | bin);
              }
          }
          return out;
      }

      private static String mergeOcrTexts(String a, String b) {
          if (a == null || a.isEmpty()) return b == null ? "" : b;
          if (b == null || b.isEmpty()) return a;
          if (a.equals(b)) return a;
          return a + "\n\n" + b;
      }

      // ─── 필드 파싱 ───────────────────────────────────────────────

      private DrawingResult parseFields(String text, String pdfName) {
          DrawingResult result = new DrawingResult();
          if (text == null || text.isEmpty()) return result;

          // OCR 글자 간 공백 정규화
          String t = text.replace('\t', ' ').replaceAll(" {2,}", " ");

          // 1) 도면번호: 파일명 우선, 없으면 OCR
          String dwgNo = null;
          if (pdfName != null) {
              Matcher fm = Pattern.compile("A\\d{8}").matcher(pdfName);
              if (fm.find()) dwgNo = fm.group();
          }
          if (dwgNo == null) {
              Matcher om = Pattern.compile("A\\d{8}").matcher(t);
              if (om.find()) dwgNo = om.group();
          }
          result.setDwgNo(dwgNo);

          // 2) 회사명
          Matcher mCo = Pattern.compile("(한화\\s*테크윈\\s*\\(\\s*[주수]\\s*\\))").matcher(t);
          if (mCo.find()) result.setCompany(mCo.group(1).replaceAll("\\s+", ""));

          // 3) 이름 필드 (글자 간 공백 허용)
          result.setWriter(extractNameAfterKeyword(t, "작성"));
          result.setDesigner(extractNameAfterKeyword(t, "설계"));
          result.setDrafter(extractNameAfterKeyword(t, "제도"));
          result.setApprover(extractNameAfterKeyword(t, "승인"));
          result.setReviewer(extractNameAfterKeyword(t, "검도"));
          if (result.getReviewer() == null)
              result.setReviewer(extractNameAfterKeyword(t, "검토"));

          // 4) 품명
          String partDesc = extractLabelValue(t, "품명");
          if (partDesc == null) partDesc = extractLabelValue(t, "도명");
          if (partDesc == null) partDesc = pickLikelyPartDescLine(t);
          result.setPartDesc(cleanPartDesc(partDesc));

          return result;
      }

      /**
       * "작 성 홍길동" / "작성:홍길동" 등 글자 간 공백·잡음이 있는 OCR에 대응.
       * extractNameAfterKeyword in PdfBottomRightIngest 이식.
       */
      private String extractNameAfterKeyword(String text, String keyword) {
          String kRegex = buildLooseKeywordRegex(keyword);
          Matcher km = Pattern.compile(kRegex).matcher(text);
          if (!km.find()) return null;

          String tail = text.substring(km.end());
          int tailMax = "작성".equals(keyword) || "설계".equals(keyword) || "제도".equals(keyword) ? 70 : 40;
          if (tail.length() > tailMax) tail = tail.substring(0, tailMax);
          tail = tail.replaceAll("(?<=[가-힣])\\s+(?=[가-힣])", ""); // 한글 글자 간 공백 제거

          // 승인자: 김xx 패턴 우선
          if ("승인".equals(keyword)) {
              Matcher mKim = Pattern.compile("김\\s*([가-힣])\\s*([가-힣])").matcher(tail);
              if (mKim.find()) {
                  String cand = "김" + mKim.group(1) + mKim.group(2);
                  if (cand.matches("[가-힣]{3}")) return cand;
              }
          }

          Matcher m = Pattern.compile("([가-힣]{2,6})").matcher(tail);
          while (m.find()) {
              String name = m.group(1).replaceAll("(님|씨)$", "");
              if (name.length() > 4) name = name.substring(0, 4);
              if (name.length() >= 3 && name.matches(".*[니N승]$"))
                  name = name.substring(0, name.length() - 1);
              if (!name.matches("[가-힣]{2,4}")) continue;
              // 라벨/부서명/품명 단어 오탐 차단
              if (name.startsWith("한화") || name.startsWith("테크") || name.startsWith("품명")) continue;
              if (name.contains("부서") || name.contains("사업") || name.contains("팀")) continue;
              if (name.contains("검도") || name.contains("승인") || name.contains("작성")
                      || name.contains("설계") || name.contains("제도")) continue;
              if (name.contains("도명") || name.contains("도번") || name.contains("도면")) continue;
              if (name.contains("조립") || name.contains("식별") || name.contains("반사")) continue;
              if (name.endsWith("용")) continue;
              return name;
          }
          return null;
      }

      /** "검 도" 같이 글자 사이 공백이 끼는 OCR을 허용하는 루즈 정규식 */
      private String buildLooseKeywordRegex(String keyword) {
          StringBuilder sb = new StringBuilder();
          for (char ch : keyword.toCharArray()) {
              sb.append(Pattern.quote(String.valueOf(ch))).append("\\s*");
          }
          return sb.toString();
      }

      /** "품명 : 브래킷트,설치용" 형태의 라벨:값 추출 (콜론 필수 — 개행 구분자 오탐 방지) */
      private String extractLabelValue(String text, String label) {
          Matcher m = Pattern.compile(
              label + "\\s*:\\s*([가-힣][가-힣A-Za-z0-9,/\\-]{1,40})"
          ).matcher(text);
          return m.find() ? m.group(1).trim() : null;
      }

      /**
       * 키워드 없을 때 폴백: 여러 줄 중 품명다운 줄 선택.
       * pickLikelyPartDescLine in PdfBottomRightIngest 이식.
       */
      private String pickLikelyPartDescLine(String raw) {
          if (raw == null) return null;
          String[] lines = raw.replace("\r\n", "\n").replace('\r', '\n').split("\n");
          String best = null;
          for (String line : lines) {
              String t = line.replaceAll("[ \\t]+", "");
              if (t.isEmpty()) continue;
              // 라벨만 있는 줄 제외
              if (t.matches("^(품명|수량|부품번호|품번|재질|도번|도명|검도|승인|작성|설계|제도)+$")) continue;
              boolean hasHangul = t.matches(".*[가-힣].*");
              boolean hasComma  = t.contains(",") || t.contains("\uFF0C");
              boolean looksLikeDesc = t.contains("패드") || t.contains("브래킷")
                      || t.contains("조립체") || t.contains("변환기") || t.contains("기용");
              if (hasHangul && (hasComma || looksLikeDesc)) return t;
              if (best == null && hasHangul && t.length() >= 3 && t.length() <= 40) best = t;
          }
          return best;
      }

      // ─── 품명 클리닝 ─────────────────────────────────────────────

      /**
       * cleanPartDesc in PdfBottomRightIngest 이식.
       * OCR 오독 보정 + 노이즈 필터링.
       */
      private String cleanPartDesc(String desc) {
          if (desc == null) return null;
          String s = desc.replace('\uFF0C', ',')
                         .replaceAll("[ \\t]+", "")
                         .replaceAll("[_|]+", "");

          // OCR 첫 글자 잘림 보정: "래킷" → "브래킷"
          s = s.replace("래킷", "브래킷").replaceAll("브{2,}래킷", "브래킷");

          // "패드" 앞 OCR 잡음 최소 매칭 (욱르지패드 → 지패드)
          String[] parts = s.split("[,\uFF0C]", 2);
          if (parts.length >= 2 && parts[0].contains("패드")) {
              Matcher m = Pattern.compile("([가-힣]{1,4}?)패드$").matcher(parts[0]);
              if (m.find()) s = m.group(1) + "패드," + parts[1];
          }

          // 너무 길면 컷
          if (s.length() > 40) s = s.substring(0, 40);
          s = s.replaceAll("[^가-힣0-9A-Za-z/,_\\-]+$", "");

          // ── 노이즈 필터 ──────────────────────────────────
          if (s.contains("승인부서") || s.endsWith("사업청") || s.endsWith("사업팀")
                  || s.equals("도명") || s.equals("도번")
                  || s.equals("부품번호") || s.equals("품번") || s.equals("품명")
                  || s.equals("수량") || s.equals("재질")) return null;
          if (s.contains("=") || s.contains("+") || s.contains(".")
                  || s.contains("도면번호") || s.contains("수기")) return null;
          if (s.contains("사업청") || s.contains("사업팀")) return null;
          if (s.contains("참조")) return null;  // "소기2참조" 등 도면 주석 오탐

          // 한글 없고 3자 이하 → 도면번호 파편 ("TS", "R0")
          boolean hasHangul = s.matches(".*[가-힣].*");
          if (!hasHangul && s.replaceAll("[^A-Za-z0-9]", "").length() <= 3) return null;

          return s.isEmpty() ? null : s;
      }

      // ─── Confidence 점수 ─────────────────────────────────────────

      private int estimateConfidence(DrawingResult result) {
          int score = 0;
          if (result.getDwgNo()    != null) score += 40;
          if (result.getCompany()  != null) score += 15;
          if (result.getApprover() != null) score += 15;
          if (result.getReviewer() != null) score += 10;
          if (result.getPartDesc() != null) score += 20;

          // 이름 형식 불일치 감점
          if (result.getApprover() != null
                  && !result.getApprover().matches("[가-힣]{2,4}")) score -= 5;
          if (result.getReviewer() != null
                  && !result.getReviewer().matches("[가-힣]{2,4}")) score -= 5;

          return Math.max(0, score);
      }
  }