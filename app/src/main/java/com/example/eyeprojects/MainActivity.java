package com.example.eyeprojects;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.InputType;
import android.view.SurfaceView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final int CAMERA_PERMISSION_CODE = 100;
    private JavaCameraView cameraView;
    private TextView resultStatus, detailStatus;
    private double defectThreshold = 5.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraView = findViewById(R.id.cameraView);
        resultStatus = findViewById(R.id.resultStatus);
        detailStatus = findViewById(R.id.detailStatus);
        TextView setThresholdButton = findViewById(R.id.setThresholdButton);

        cameraView.setVisibility(SurfaceView.VISIBLE);
        cameraView.setCameraIndex(0);
        cameraView.setCvCameraViewListener(this);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            initOpenCV();
        }

        setThresholdButton.setOnClickListener(v -> showThresholdInputDialog());
    }

    private void initOpenCV() {
        if (OpenCVLoader.initDebug()) {
            cameraView.setCameraPermissionGranted();
            cameraView.enableView();
        } else {
            Toast.makeText(this, "OpenCV yüklenemedi", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initOpenCV();
        } else {
            Toast.makeText(this, "Kamera izni gerekli!", Toast.LENGTH_LONG).show();
        }
    }

    private void showThresholdInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Hata Eşiği Belirle");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(String.format(Locale.US, "%.1f", defectThreshold));
        builder.setView(input);

        builder.setPositiveButton("Kaydet", (dialog, which) -> {
            try {
                double newThreshold = Double.parseDouble(input.getText().toString());
                if (newThreshold >= 0) {
                    defectThreshold = newThreshold;
                    Toast.makeText(this, "Hata eşiği: " + String.format(Locale.US, "%.1f", defectThreshold) + "% olarak ayarlandı", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Eşik değeri negatif olamaz.", Toast.LENGTH_SHORT).show();
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Geçersiz eşik değeri!", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("İptal", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
    }

    @Override
    public void onCameraViewStopped() {
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat rgbaFull = inputFrame.rgba();
        Mat rgba = new Mat();
        Mat gray = new Mat();
        Mat edges = new Mat();
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();

        List<MatOfPoint> dibujarContours = new ArrayList<>();
        Mat defectHierarchy = new Mat();

        Mat mask = null, diff = null, defectMask = null;
        MatOfDouble meanDev = null, stddev = null;
        MatOfInt hull = null;
        MatOfPoint smoothedContour = null, scaledContour = null; // hullPoints kaldırıldı, doğrudan kullanılmayacak
        MatOfPoint2f contour2f = null, smoothedContour2f = null, approxRect = null;
        MatOfInt4 defectsMat = null;

        double scaleFactor = 0.5;
        Size scaledSize = new Size(rgbaFull.cols() * scaleFactor, rgbaFull.rows() * scaleFactor);

        try {
            Imgproc.resize(rgbaFull, rgba, scaledSize, 0, 0, Imgproc.INTER_LINEAR);
            Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY);
            Imgproc.GaussianBlur(gray, gray, new Size(9, 9), 0);
            Imgproc.Canny(gray, edges, 50, 150);

            Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

            MatOfPoint selectedContour = null;
            double maxArea = 0;

            for (MatOfPoint contour : contours) {
                double area = Imgproc.contourArea(contour);
                Rect rect = Imgproc.boundingRect(contour);
                float aspect = (rect.width == 0 || rect.height == 0) ? 0 : (float) rect.width / rect.height;

                // Alan eşiğini ölçek faktörüne göre ayarla
                if (area > (8000 * scaleFactor * scaleFactor) && aspect > 0.5 && aspect < 2.0) {
                    if (area > maxArea) {
                        maxArea = area;
                        selectedContour = contour;
                    }
                }
            }

            if (selectedContour != null) {
                // Yumuşatılmış kontur (alan ve leke analizi için)
                contour2f = new MatOfPoint2f(selectedContour.toArray());
                smoothedContour2f = new MatOfPoint2f();
                Imgproc.approxPolyDP(contour2f, smoothedContour2f, 0.04 * Imgproc.arcLength(contour2f, true), true);
                smoothedContour = new MatOfPoint(smoothedContour2f.toArray());

                // totalArea, yumuşatılmış konturdan hesaplanıyor (orijinal mantık)
                mask = Mat.zeros(gray.size(), CvType.CV_8UC1);
                Imgproc.drawContours(mask, List.of(smoothedContour), -1, new Scalar(255), Core.FILLED);
                double totalArea = Core.countNonZero(mask);

                // Leke Analizi
                Scalar meanGray = Core.mean(gray, mask);
                diff = new Mat();
                Core.absdiff(gray, new Scalar(meanGray.val[0]), diff);
                Imgproc.threshold(diff, diff, 45, 255, Imgproc.THRESH_BINARY);
                defectMask = new Mat();
                Core.bitwise_and(diff, mask, defectMask);
                double defectArea = Core.countNonZero(defectMask);
                double lekeOrani = (totalArea > 0) ? (defectArea / totalArea) * 100 : 0;

                Mat tempDefectMask = defectMask.clone();
                Imgproc.findContours(tempDefectMask, dibujarContours, defectHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
                tempDefectMask.release();

                for (MatOfPoint defectContourItem : dibujarContours) {
                    MatOfPoint scaledDefectContourItem = new MatOfPoint();
                    List<Point> scaledDefectPoints = new ArrayList<>();
                    for (Point p : defectContourItem.toList()) {
                        scaledDefectPoints.add(new Point(p.x / scaleFactor, p.y / scaleFactor));
                    }
                    scaledDefectContourItem.fromList(scaledDefectPoints);
                    Imgproc.drawContours(rgbaFull, List.of(scaledDefectContourItem), -1, new Scalar(255, 0, 0), 2);
                    scaledDefectContourItem.release();
                }

                // Deformasyon Analizi
                meanDev = new MatOfDouble();
                stddev = new MatOfDouble();
                Core.meanStdDev(gray, meanDev, stddev, mask); // mask hala smoothedContour'a ait
                double deformasyon = Math.min((stddev.get(0,0)[0] / 250.0) * 100.0, 100.0);


                // Simetri Analizi (yumuşatılmış kontur üzerinden)
                double horizDiff = 0, vertDiff = 0;
                approxRect = new MatOfPoint2f(); // contour2f (selectedContour'dan) ile çalışabilir veya smoothedContour2f
                Imgproc.approxPolyDP(smoothedContour2f, approxRect, 0.04 * Imgproc.arcLength(smoothedContour2f, true), true);

                if (approxRect.total() == 4) {
                    Point[] pts = approxRect.toArray();
                    double d1 = Math.hypot(pts[0].x - pts[1].x, pts[0].y - pts[1].y);
                    double d3 = Math.hypot(pts[2].x - pts[3].x, pts[2].y - pts[3].y);
                    double d2 = Math.hypot(pts[1].x - pts[2].x, pts[1].y - pts[2].y);
                    double d4 = Math.hypot(pts[3].x - pts[0].x, pts[3].y - pts[0].y);

                    horizDiff = (Math.max(d1, d3) == 0) ? 100 : Math.abs(d1 - d3) / Math.max(d1, d3) * 100;
                    vertDiff = (Math.max(d2, d4) == 0) ? 100 : Math.abs(d2 - d4) / Math.max(d2, d4) * 100;
                } else {
                    horizDiff = vertDiff = 100;
                }

                // --- Yırtık Analizi (Orijinal Mantığa Geri Dönüş) ---
                hull = new MatOfInt();
                // Convex Hull'ı selectedContour (ham kontur) üzerinden hesapla
                Imgproc.convexHull(selectedContour, hull);

                // Defect noktalarını selectedContour'dan al
                Point[] selectedContourPoints = selectedContour.toArray();

                defectsMat = new MatOfInt4();
                // Convexity Defects'i selectedContour ve onun hull'ı ile hesapla
                if (selectedContour.rows() > 3 && hull.rows() > 0 && !hull.empty()) {
                    Imgproc.convexityDefects(selectedContour, hull, defectsMat);
                }

                double tearDefectScore = 0;
                if (defectsMat != null && !defectsMat.empty() && defectsMat.rows() > 0) {
                    for (int i = 0; i < defectsMat.rows(); ++i) {
                        double[] vec = defectsMat.get(i, 0);
                        if (vec != null && vec.length == 4) {

                            // Bu indisler selectedContourPoints dizisine aittir
                            Point ptStart = selectedContourPoints[(int) vec[0]];
                            Point ptEnd = selectedContourPoints[(int) vec[1]];
                            Point ptFar = selectedContourPoints[(int) vec[2]];
                            double depth = vec[3] / 256.0;

                            // Orijinal derinlik eşiği
                            if (depth > 5.0) {
                                tearDefectScore += depth;
                                // Yırtık görselleştirme (ölçekli koordinatlarla)
                                Point scaledPtStart = new Point(ptStart.x / scaleFactor, ptStart.y / scaleFactor);
                                Point scaledPtEnd = new Point(ptEnd.x / scaleFactor, ptEnd.y / scaleFactor);
                                Point scaledPtFar = new Point(ptFar.x / scaleFactor, ptFar.y / scaleFactor);

                                Imgproc.line(rgbaFull, scaledPtStart, scaledPtFar, new Scalar(255, 165, 0), 2);
                                Imgproc.line(rgbaFull, scaledPtEnd, scaledPtFar, new Scalar(255, 165, 0), 2);
                                Imgproc.circle(rgbaFull, scaledPtFar, 5, new Scalar(255, 0, 255), -1);
                            }
                        }
                    }
                }
                // Normalizasyon totalArea (smoothedContour'dan) ile yapılıyor (orijinal mantık)
                double normalizedTearScore = (totalArea > 0) ? Math.min((tearDefectScore / (totalArea / 1000.0)) * 50.0, 100.0) : 0;


                double toplamHata = (lekeOrani * 0.05) + (deformasyon * 0.45) + ((horizDiff + vertDiff) / 2.0 * 0.25) + (normalizedTearScore * 0.25);

                // Ana konturu çiz (yumuşatılmış olanı çizmek daha iyi görünebilir)
                scaledContour = new MatOfPoint();
                List<Point> scaledPoints = new ArrayList<>();
                for (Point p : smoothedContour.toList()) {
                    scaledPoints.add(new Point(p.x / scaleFactor, p.y / scaleFactor));
                }
                scaledContour.fromList(scaledPoints);
                Imgproc.drawContours(rgbaFull, List.of(scaledContour), -1, new Scalar(0, 255, 0), 3);

                final double finalToplamHata = toplamHata;
                final double finalLekeOrani = lekeOrani;
                final double finalDeformasyon = deformasyon;
                final double finalHorizDiff = horizDiff;
                final double finalVertDiff = vertDiff;
                final double finalNormalizedTearScore = normalizedTearScore;

                runOnUiThread(() -> {
                    if (finalToplamHata > defectThreshold) {
                        resultStatus.setText(String.format(Locale.US,"KALDI (%.2f%% hata)", finalToplamHata));
                        resultStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    } else {
                        resultStatus.setText(String.format(Locale.US,"GEÇTİ (%.2f%% hata)", finalToplamHata));
                        resultStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                    }

                    if (defectThreshold == 0 && finalToplamHata > 0) {
                        resultStatus.setText("Yüzde 0 hatalı ürün bulunmamaktadır");
                        resultStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                    }

                    String rapor = String.format(Locale.US,
                            "Detay:\n• Leke: %.1f%%\n• Ambalaj Bozulması: %.1f%%\n• Kenar Simetrisi Yatay: %.1f%%\n• Kenar Simetrisi Dikey: %.1f%%\n• Yırtık: %.1f%%",
                            finalLekeOrani, finalDeformasyon, finalHorizDiff, finalVertDiff, finalNormalizedTearScore
                    );
                    detailStatus.setText(rapor);
                });

            } else {
                runOnUiThread(() -> {
                    resultStatus.setText("Ürün algılanmadı");
                    resultStatus.setTextColor(getResources().getColor(android.R.color.darker_gray));
                    detailStatus.setText("Ürün algılanması bekleniyor...");
                });
            }
        } catch (Exception e) {
            // e.printStackTrace(); // For debugging
            runOnUiThread(() -> {
                resultStatus.setText("Hata oluştu");
                detailStatus.setText(e.getMessage() != null ? e.getMessage() : "Bilinmeyen hata");
            });
        }
        finally {
            rgba.release();
            gray.release();
            edges.release();
            hierarchy.release();
            defectHierarchy.release();

            if (mask != null) mask.release();
            if (diff != null) diff.release();
            if (defectMask != null) defectMask.release();
            if (meanDev != null) meanDev.release();
            if (stddev != null) stddev.release();
            if (hull != null) hull.release();
            if (defectsMat != null) defectsMat.release();

            if (contour2f != null) contour2f.release();
            if (smoothedContour2f != null) smoothedContour2f.release();
            if (smoothedContour != null) smoothedContour.release();
            if (approxRect != null) approxRect.release();
            if (scaledContour != null) scaledContour.release();


            for (MatOfPoint contour : contours) {
                if (contour != null) contour.release();
            }
            contours.clear();

            for (MatOfPoint defectContourItem : dibujarContours) {
                if (defectContourItem != null) defectContourItem.release();
            }
            dibujarContours.clear();
        }
        return rgbaFull;
    }
}