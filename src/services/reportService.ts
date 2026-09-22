import { jsPDF } from 'jspdf';
import { IncidentReport, TimelineEvent } from '../types';

export function generateIncidentPdf(report: IncidentReport): { blob: Blob; filename: string } {
  const doc = new jsPDF({
    orientation: 'portrait',
    unit: 'mm',
    format: 'a4',
  });

  const pageWidth = doc.internal.pageSize.getWidth();
  let y = 20;

  // Header Banner
  doc.setFillColor(7, 21, 44); // Midnight Blue #07152C
  doc.rect(0, 0, pageWidth, 35, 'F');

  doc.setTextColor(255, 255, 255);
  doc.setFontSize(22);
  doc.setFont('helvetica', 'bold');
  doc.text('SafeRescue Incident Report', 14, 18);

  doc.setFontSize(10);
  doc.setFont('helvetica', 'normal');
  doc.setTextColor(184, 196, 217);
  doc.text('Confidential Emergency Summary • Local Verification Record', 14, 26);

  y = 45;

  // Metadata Card
  doc.setFillColor(245, 247, 250);
  doc.roundedRect(14, y, pageWidth - 28, 48, 3, 3, 'F');

  doc.setTextColor(33, 37, 41);
  doc.setFontSize(12);
  doc.setFont('helvetica', 'bold');
  doc.text('INCIDENT OVERVIEW', 20, y + 8);

  doc.setFontSize(9);
  doc.setFont('helvetica', 'normal');
  doc.setTextColor(80, 85, 95);

  const genDate = new Date(report.generatedAtMillis).toLocaleString();
  doc.text(`Incident ID: ${report.incidentId}`, 20, y + 16);
  doc.text(`Generated: ${genDate}`, 20, y + 22);
  doc.text(`Evidence Attached: ${report.evidenceCount} item(s)`, 20, y + 28);
  doc.text(`Voice Monitoring: ${report.voiceMonitoringUsed ? 'Active (Acoustic RMS Logged)' : 'None'}`, 20, y + 34);

  // Risk & GPS on Right Column
  const col2X = pageWidth / 2 + 10;
  doc.setFont('helvetica', 'bold');
  doc.text('Risk Score Assessment:', col2X, y + 16);
  doc.setFont('helvetica', 'normal');
  doc.text(`${report.finalRiskScore} / 100 (${report.finalRiskSeverity})`, col2X, y + 22);

  doc.setFont('helvetica', 'bold');
  doc.text('Location Coordinates:', col2X, y + 28);
  doc.setFont('helvetica', 'normal');
  if (report.latestLocation) {
    doc.text(
      `${report.latestLocation.latitude.toFixed(6)}, ${report.latestLocation.longitude.toFixed(6)} (±${Math.round(report.latestLocation.accuracyMeters)}m)`,
      col2X,
      y + 34
    );
  } else {
    doc.text('No GPS fix available during incident', col2X, y + 34);
  }

  y += 58;

  // Timeline Section
  doc.setTextColor(7, 21, 44);
  doc.setFontSize(14);
  doc.setFont('helvetica', 'bold');
  doc.text('Chronological Incident Timeline', 14, y);
  y += 6;

  doc.setDrawColor(220, 225, 230);
  doc.line(14, y, pageWidth - 14, y);
  y += 6;

  if (report.timeline.length === 0) {
    doc.setFontSize(10);
    doc.setFont('helvetica', 'italic');
    doc.setTextColor(120, 125, 135);
    doc.text('No chronological events recorded.', 14, y + 6);
    y += 12;
  } else {
    report.timeline.forEach((event: TimelineEvent) => {
      if (y > 270) {
        doc.addPage();
        y = 20;
      }

      const eventTime = new Date(event.timestampMillis).toLocaleTimeString([], {
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
      });

      // Timeline marker dot
      doc.setFillColor(91, 75, 219); // #5B4BDB
      doc.circle(18, y + 1.5, 2, 'F');

      doc.setFontSize(9);
      doc.setFont('helvetica', 'bold');
      doc.setTextColor(30, 35, 45);
      doc.text(`${eventTime} — ${event.title}`, 24, y + 2.5);

      if (event.riskScore != null) {
        doc.setFontSize(8);
        doc.setTextColor(229, 30, 77); // #E51E4D
        doc.text(`[Risk: ${event.riskScore}/100]`, pageWidth - 45, y + 2.5);
      }

      doc.setFontSize(8.5);
      doc.setFont('helvetica', 'normal');
      doc.setTextColor(90, 95, 105);
      const splitText = doc.splitTextToSize(event.detail, pageWidth - 45);
      doc.text(splitText, 24, y + 7);

      y += 8 + splitText.length * 3.5;
    });
  }

  // Footer Disclaimer
  const footerY = 285;
  doc.setFontSize(7.5);
  doc.setFont('helvetica', 'normal');
  doc.setTextColor(140, 145, 155);
  doc.text(
    'Notice: SafeRescue incident logs are stored encrypted and locally on this device. Not a certified police dispatch record.',
    14,
    footerY
  );

  const filename = `SafeRescue-Incident-${report.incidentId}.pdf`;
  const blob = doc.output('blob');
  return { blob, filename };
}
