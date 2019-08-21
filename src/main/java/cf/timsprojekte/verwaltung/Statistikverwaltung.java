package cf.timsprojekte.verwaltung;

import cf.timsprojekte.UniqueBot;
import cf.timsprojekte.verwaltung.immutable.Statistik;
import org.jfree.chart.*;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.*;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.telegram.abilitybots.api.db.DBContext;

import javax.validation.constraints.NotNull;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public class Statistikverwaltung {

    private Set<Statistik> statistikSet;

    public Statistikverwaltung(DBContext db) {
        statistikSet = db.getSet("Statistik");
        StandardChartTheme theme = new StandardChartTheme("Custom", false);
        /*Font fonts[] = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
        Font font = null;
        for (int i = 0; i < fonts.length; i++) {
            if (fonts[i].getName().equals("Comic Sans MS"))
                font = fonts[i];
        }*/
        String fontName = "Comic Sans MS";
        theme.setBarPainter(new GradientBarPainter());
        theme.setTitlePaint( Color.decode( "#4572a7" ) );
        theme.setExtraLargeFont( new Font(fontName,Font.PLAIN, 16) );
        theme.setLargeFont( new Font(fontName,Font.BOLD, 15));
        theme.setRegularFont( new Font(fontName,Font.PLAIN, 11));
        theme.setRangeGridlinePaint( Color.decode("#C0C0C0"));
        theme.setPlotBackgroundPaint( Color.decode("#FFFFFF") );
        theme.setChartBackgroundPaint( Color.decode("#FFFFFF"));
        theme.setGridBandPaint( Color.red );
        theme.setAxisOffset( new RectangleInsets(0,0,0,0) );
        theme.setBarPainter(new StandardBarPainter());
        theme.setAxisLabelPaint( Color.decode("#666666")  );
        ChartFactory.setChartTheme(theme);
    }

    public Statistik getStatistik(long userId) {
        Optional<Statistik> benutzer = statistikSet.stream().filter(b -> b.getUserId() == userId).findAny();
        if (benutzer.isPresent())
            return benutzer.get();
        else {
            Statistik statistik = new Statistik(userId, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
            createStatistik(statistik);
            return statistik;
        }
    }

    public void createStatistik(@NotNull Statistik benutzer) {
        if (statistikSet.contains(benutzer)) return;
        statistikSet.add(benutzer);
    }

    public Statistik replace(@NotNull Statistik vorher, @NotNull Statistik nachher) {
        if (statistikSet.remove(vorher)) {
            if (statistikSet.add(nachher)) {
                return nachher;
            }
            statistikSet.add(vorher);
        }
        return vorher;
    }

    public Statistik logText(Statistik statistik) {
        return replace(statistik, new Statistik(statistik.getUserId(), statistik.getText() + 1, statistik.getSticker(), statistik.getPhoto(), statistik.getVideo(), statistik.getVoice(), statistik.getGot(), statistik.getLost(), statistik.getWon(), statistik.getSpent(), statistik.getMonday(), statistik.getTuesday(), statistik.getWednesday(), statistik.getThursday(), statistik.getFriday(), statistik.getSaturaday(), statistik.getSunday(), statistik.getNight(), statistik.getMorning(), statistik.getDay(), statistik.getEvening(), statistik.getLikeIn(), statistik.getLikeOut(), statistik.getRewards(), statistik.getDailys()));
    }

    public Statistik logSticker(Statistik statistik) {
        return replace(statistik, new Statistik(statistik.getUserId(), statistik.getText(), statistik.getSticker() + 1, statistik.getPhoto(), statistik.getVideo(), statistik.getVoice(), statistik.getGot(), statistik.getLost(), statistik.getWon(), statistik.getSpent(), statistik.getMonday(), statistik.getTuesday(), statistik.getWednesday(), statistik.getThursday(), statistik.getFriday(), statistik.getSaturaday(), statistik.getSunday(), statistik.getNight(), statistik.getMorning(), statistik.getDay(), statistik.getEvening(), statistik.getLikeIn(), statistik.getLikeOut(), statistik.getRewards(), statistik.getDailys()));
    }

    public Statistik logPhoto(Statistik statistik) {
        return replace(statistik, new Statistik(statistik.getUserId(), statistik.getText(), statistik.getSticker(), statistik.getPhoto() + 1, statistik.getVideo(), statistik.getVoice(), statistik.getGot(), statistik.getLost(), statistik.getWon(), statistik.getSpent(), statistik.getMonday(), statistik.getTuesday(), statistik.getWednesday(), statistik.getThursday(), statistik.getFriday(), statistik.getSaturaday(), statistik.getSunday(), statistik.getNight(), statistik.getMorning(), statistik.getDay(), statistik.getEvening(), statistik.getLikeIn(), statistik.getLikeOut(), statistik.getRewards(), statistik.getDailys()));
    }

    public Statistik logVideo(Statistik statistik) {
        return replace(statistik, new Statistik(statistik.getUserId(), statistik.getText(), statistik.getSticker(), statistik.getPhoto(), statistik.getVideo() + 1, statistik.getVoice(), statistik.getGot(), statistik.getLost(), statistik.getWon(), statistik.getSpent(), statistik.getMonday(), statistik.getTuesday(), statistik.getWednesday(), statistik.getThursday(), statistik.getFriday(), statistik.getSaturaday(), statistik.getSunday(), statistik.getNight(), statistik.getMorning(), statistik.getDay(), statistik.getEvening(), statistik.getLikeIn(), statistik.getLikeOut(), statistik.getRewards(), statistik.getDailys()));
    }

    public Statistik logVoice(Statistik statistik) {
        return replace(statistik, new Statistik(statistik.getUserId(), statistik.getText(), statistik.getSticker(), statistik.getPhoto(), statistik.getVideo(), statistik.getVoice() + 1, statistik.getGot(), statistik.getLost(), statistik.getWon(), statistik.getSpent(), statistik.getMonday(), statistik.getTuesday(), statistik.getWednesday(), statistik.getThursday(), statistik.getFriday(), statistik.getSaturaday(), statistik.getSunday(), statistik.getNight(), statistik.getMorning(), statistik.getDay(), statistik.getEvening(), statistik.getLikeIn(), statistik.getLikeOut(), statistik.getRewards(), statistik.getDailys()));
    }

    public Statistik logGot(Statistik statistik) {
        return replace(statistik, new Statistik(statistik.getUserId(), statistik.getText(), statistik.getSticker(), statistik.getPhoto(), statistik.getVideo(), statistik.getVoice(), statistik.getGot() + 1, statistik.getLost(), statistik.getWon(), statistik.getSpent(), statistik.getMonday(), statistik.getTuesday(), statistik.getWednesday(), statistik.getThursday(), statistik.getFriday(), statistik.getSaturaday(), statistik.getSunday(), statistik.getNight(), statistik.getMorning(), statistik.getDay(), statistik.getEvening(), statistik.getLikeIn(), statistik.getLikeOut(), statistik.getRewards(), statistik.getDailys()));
    }

    public Statistik logLost(Statistik statistik) {
        return replace(statistik, new Statistik(statistik.getUserId(), statistik.getText(), statistik.getSticker(), statistik.getPhoto(), statistik.getVideo(), statistik.getVoice(), statistik.getGot(), statistik.getLost() + 1, statistik.getWon(), statistik.getSpent(), statistik.getMonday(), statistik.getTuesday(), statistik.getWednesday(), statistik.getThursday(), statistik.getFriday(), statistik.getSaturaday(), statistik.getSunday(), statistik.getNight(), statistik.getMorning(), statistik.getDay(), statistik.getEvening(), statistik.getLikeIn(), statistik.getLikeOut(), statistik.getRewards(), statistik.getDailys()));
    }

    public Statistik logWon(Statistik statistik) {
        return replace(statistik, new Statistik(statistik.getUserId(), statistik.getText(), statistik.getSticker(), statistik.getPhoto(), statistik.getVideo(), statistik.getVoice(), statistik.getGot(), statistik.getLost(), statistik.getWon() + 1, statistik.getSpent(), statistik.getMonday(), statistik.getTuesday(), statistik.getWednesday(), statistik.getThursday(), statistik.getFriday(), statistik.getSaturaday(), statistik.getSunday(), statistik.getNight(), statistik.getMorning(), statistik.getDay(), statistik.getEvening(), statistik.getLikeIn(), statistik.getLikeOut(), statistik.getRewards(), statistik.getDailys()));
    }

    public Statistik logSpent(Statistik statistik) {
        return replace(statistik, new Statistik(statistik.getUserId(), statistik.getText(), statistik.getSticker(), statistik.getPhoto(), statistik.getVideo(), statistik.getVoice(), statistik.getGot(), statistik.getLost(), statistik.getWon(), statistik.getSpent() + 1, statistik.getMonday(), statistik.getTuesday(), statistik.getWednesday(), statistik.getThursday(), statistik.getFriday(), statistik.getSaturaday(), statistik.getSunday(), statistik.getNight(), statistik.getMorning(), statistik.getDay(), statistik.getEvening(), statistik.getLikeIn(), statistik.getLikeOut(), statistik.getRewards(), statistik.getDailys()));
    }

    public Statistik logMonday(Statistik statistik) {
        return replace(statistik, new Statistik(statistik.getUserId(), statistik.getText(), statistik.getSticker(), statistik.getPhoto(), statistik.getVideo(), statistik.getVoice(), statistik.getGot(), statistik.getLost(), statistik.getWon(), statistik.getSpent(), statistik.getMonday() + 1, statistik.getTuesday(), statistik.getWednesday(), statistik.getThursday(), statistik.getFriday(), statistik.getSaturaday(), statistik.getSunday(), statistik.getNight(), statistik.getMorning(), statistik.getDay(), statistik.getEvening(), statistik.getLikeIn(), statistik.getLikeOut(), statistik.getRewards(), statistik.getDailys()));
    }

    public Statistik logTuesday(Statistik statistik) {
        return replace(statistik, new Statistik(statistik.getUserId(), statistik.getText(), statistik.getSticker(), statistik.getPhoto(), statistik.getVideo(), statistik.getVoice(), statistik.getGot(), statistik.getLost(), statistik.getWon(), statistik.getSpent(), statistik.getMonday(), statistik.getTuesday() + 1, statistik.getWednesday(), statistik.getThursday(), statistik.getFriday(), statistik.getSaturaday(), statistik.getSunday(), statistik.getNight(), statistik.getMorning(), statistik.getDay(), statistik.getEvening(), statistik.getLikeIn(), statistik.getLikeOut(), statistik.getRewards(), statistik.getDailys()));
    }

    public Statistik logWednesday(Statistik statistik) {
        return replace(statistik, new Statistik(statistik.getUserId(), statistik.getText(), statistik.getSticker(), statistik.getPhoto(), statistik.getVideo(), statistik.getVoice(), statistik.getGot(), statistik.getLost(), statistik.getWon(), statistik.getSpent(), statistik.getMonday(), statistik.getTuesday(), statistik.getWednesday() + 1, statistik.getThursday(), statistik.getFriday(), statistik.getSaturaday(), statistik.getSunday(), statistik.getNight(), statistik.getMorning(), statistik.getDay(), statistik.getEvening(), statistik.getLikeIn(), statistik.getLikeOut(), statistik.getRewards(), statistik.getDailys()));
    }

    public Statistik logThursday(Statistik statistik) {
        return replace(statistik, new Statistik(statistik.getUserId(), statistik.getText(), statistik.getSticker(), statistik.getPhoto(), statistik.getVideo(), statistik.getVoice(), statistik.getGot(), statistik.getLost(), statistik.getWon(), statistik.getSpent(), statistik.getMonday(), statistik.getTuesday(), statistik.getWednesday(), statistik.getThursday() + 1, statistik.getFriday(), statistik.getSaturaday(), statistik.getSunday(), statistik.getNight(), statistik.getMorning(), statistik.getDay(), statistik.getEvening(), statistik.getLikeIn(), statistik.getLikeOut(), statistik.getRewards(), statistik.getDailys()));
    }

    public Statistik logFriday(Statistik statistik) {
        return replace(statistik, new Statistik(statistik.getUserId(), statistik.getText(), statistik.getSticker(), statistik.getPhoto(), statistik.getVideo(), statistik.getVoice(), statistik.getGot(), statistik.getLost(), statistik.getWon(), statistik.getSpent(), statistik.getMonday(), statistik.getTuesday(), statistik.getWednesday(), statistik.getThursday(), statistik.getFriday() + 1, statistik.getSaturaday(), statistik.getSunday(), statistik.getNight(), statistik.getMorning(), statistik.getDay(), statistik.getEvening(), statistik.getLikeIn(), statistik.getLikeOut(), statistik.getRewards(), statistik.getDailys()));
    }

    public Statistik logSaturday(Statistik statistik) {
        return replace(statistik, new Statistik(statistik.getUserId(), statistik.getText(), statistik.getSticker(), statistik.getPhoto(), statistik.getVideo(), statistik.getVoice(), statistik.getGot(), statistik.getLost(), statistik.getWon(), statistik.getSpent(), statistik.getMonday(), statistik.getTuesday(), statistik.getWednesday(), statistik.getThursday(), statistik.getFriday(), statistik.getSaturaday() + 1, statistik.getSunday(), statistik.getNight(), statistik.getMorning(), statistik.getDay(), statistik.getEvening(), statistik.getLikeIn(), statistik.getLikeOut(), statistik.getRewards(), statistik.getDailys()));
    }

    public Statistik logSunday(Statistik statistik) {
        return replace(statistik, new Statistik(statistik.getUserId(), statistik.getText(), statistik.getSticker(), statistik.getPhoto(), statistik.getVideo(), statistik.getVoice(), statistik.getGot(), statistik.getLost(), statistik.getWon(), statistik.getSpent(), statistik.getMonday(), statistik.getTuesday(), statistik.getWednesday(), statistik.getThursday(), statistik.getFriday(), statistik.getSaturaday(), statistik.getSunday() + 1, statistik.getNight(), statistik.getMorning(), statistik.getDay(), statistik.getEvening(), statistik.getLikeIn(), statistik.getLikeOut(), statistik.getRewards(), statistik.getDailys()));
    }

    public Statistik logNight(Statistik statistik) {
        return replace(statistik, new Statistik(statistik.getUserId(), statistik.getText(), statistik.getSticker(), statistik.getPhoto(), statistik.getVideo(), statistik.getVoice(), statistik.getGot(), statistik.getLost(), statistik.getWon(), statistik.getSpent(), statistik.getMonday(), statistik.getTuesday(), statistik.getWednesday(), statistik.getThursday(), statistik.getFriday(), statistik.getSaturaday(), statistik.getSunday(), statistik.getNight() + 1, statistik.getMorning(), statistik.getDay(), statistik.getEvening(), statistik.getLikeIn(), statistik.getLikeOut(), statistik.getRewards(), statistik.getDailys()));
    }

    public Statistik logMorning(Statistik statistik) {
        return replace(statistik, new Statistik(statistik.getUserId(), statistik.getText(), statistik.getSticker(), statistik.getPhoto(), statistik.getVideo(), statistik.getVoice(), statistik.getGot(), statistik.getLost(), statistik.getWon(), statistik.getSpent(), statistik.getMonday(), statistik.getTuesday(), statistik.getWednesday(), statistik.getThursday(), statistik.getFriday(), statistik.getSaturaday(), statistik.getSunday(), statistik.getNight(), statistik.getMorning() + 1, statistik.getDay(), statistik.getEvening(), statistik.getLikeIn(), statistik.getLikeOut(), statistik.getRewards(), statistik.getDailys()));
    }

    public Statistik logDay(Statistik statistik) {
        return replace(statistik, new Statistik(statistik.getUserId(), statistik.getText(), statistik.getSticker(), statistik.getPhoto(), statistik.getVideo(), statistik.getVoice(), statistik.getGot(), statistik.getLost(), statistik.getWon(), statistik.getSpent(), statistik.getMonday(), statistik.getTuesday(), statistik.getWednesday(), statistik.getThursday(), statistik.getFriday(), statistik.getSaturaday(), statistik.getSunday(), statistik.getNight(), statistik.getMorning(), statistik.getDay() + 1, statistik.getEvening(), statistik.getLikeIn(), statistik.getLikeOut(), statistik.getRewards(), statistik.getDailys()));
    }

    public Statistik logEvening(Statistik statistik) {
        return replace(statistik, new Statistik(statistik.getUserId(), statistik.getText(), statistik.getSticker(), statistik.getPhoto(), statistik.getVideo(), statistik.getVoice(), statistik.getGot(), statistik.getLost(), statistik.getWon(), statistik.getSpent(), statistik.getMonday(), statistik.getTuesday(), statistik.getWednesday(), statistik.getThursday(), statistik.getFriday(), statistik.getSaturaday(), statistik.getSunday(), statistik.getNight(), statistik.getMorning(), statistik.getDay(), statistik.getEvening() + 1, statistik.getLikeIn(), statistik.getLikeOut(), statistik.getRewards(), statistik.getDailys()));
    }

    public Statistik logLikeIn(Statistik statistik) {
        return replace(statistik, new Statistik(statistik.getUserId(), statistik.getText(), statistik.getSticker(), statistik.getPhoto(), statistik.getVideo(), statistik.getVoice(), statistik.getGot(), statistik.getLost(), statistik.getWon(), statistik.getSpent(), statistik.getMonday(), statistik.getTuesday(), statistik.getWednesday(), statistik.getThursday(), statistik.getFriday(), statistik.getSaturaday(), statistik.getSunday(), statistik.getNight(), statistik.getMorning(), statistik.getDay(), statistik.getEvening(), statistik.getLikeIn() + 1, statistik.getLikeOut(), statistik.getRewards(), statistik.getDailys()));
    }

    public Statistik logLikeOut(Statistik statistik) {
        return replace(statistik, new Statistik(statistik.getUserId(), statistik.getText(), statistik.getSticker(), statistik.getPhoto(), statistik.getVideo(), statistik.getVoice(), statistik.getGot(), statistik.getLost(), statistik.getWon(), statistik.getSpent(), statistik.getMonday(), statistik.getTuesday(), statistik.getWednesday(), statistik.getThursday(), statistik.getFriday(), statistik.getSaturaday(), statistik.getSunday(), statistik.getNight(), statistik.getMorning(), statistik.getDay(), statistik.getEvening(), statistik.getLikeIn(), statistik.getLikeOut() + 1, statistik.getRewards(), statistik.getDailys()));
    }

    public Statistik logRewards(Statistik statistik) {
        return replace(statistik, new Statistik(statistik.getUserId(), statistik.getText(), statistik.getSticker(), statistik.getPhoto(), statistik.getVideo(), statistik.getVoice(), statistik.getGot(), statistik.getLost(), statistik.getWon(), statistik.getSpent(), statistik.getMonday(), statistik.getTuesday(), statistik.getWednesday(), statistik.getThursday(), statistik.getFriday(), statistik.getSaturaday(), statistik.getSunday(), statistik.getNight(), statistik.getMorning(), statistik.getDay(), statistik.getEvening(), statistik.getLikeIn(), statistik.getLikeOut(), statistik.getRewards() + 1, statistik.getDailys()));
    }

    public Statistik logDailys(Statistik statistik) {
        return replace(statistik, new Statistik(statistik.getUserId(), statistik.getText(), statistik.getSticker(), statistik.getPhoto(), statistik.getVideo(), statistik.getVoice(), statistik.getGot(), statistik.getLost(), statistik.getWon(), statistik.getSpent(), statistik.getMonday(), statistik.getTuesday(), statistik.getWednesday(), statistik.getThursday(), statistik.getFriday(), statistik.getSaturaday(), statistik.getSunday(), statistik.getNight(), statistik.getMorning(), statistik.getDay(), statistik.getEvening(), statistik.getLikeIn(), statistik.getLikeOut(), statistik.getRewards(), statistik.getDailys() + 1));
    }

    public BufferedImage generateTextPie() {
        return generatePie("Textnachrichten gesendet", Statistik::getText);
    }

    public BufferedImage generateStickerPie() {
        return generatePie("Sticker gesendet", Statistik::getSticker);
    }

    public BufferedImage generatePhotoPie() {
        return generatePie("Fotos gesendet", Statistik::getPhoto);
    }

    public BufferedImage generateVideoPie() {
        return generatePie("Videos gesendet", Statistik::getVideo);
    }

    public BufferedImage generateVoicePie() {
        return generatePie("Sprachnachrichten gesendet", Statistik::getVoice);
    }

    public BufferedImage generateLikeInPie() {
        return generatePie("Ehrungen empfangen", Statistik::getLikeIn);
    }

    public BufferedImage generateLikeOutPie() {
        return generatePie("Ehrungen gesendet", Statistik::getLikeOut);
    }

    private BufferedImage generatePie(String titel, Function<Statistik, Integer> function) {
        DefaultPieDataset data = new DefaultPieDataset();
        Nutzerverwaltung n = UniqueBot.unique().nutzerverwaltung;
        statistikSet.forEach(statistik -> data.insertValue(0, n.getBenutzer(statistik.getUserId()).getNutzername(), function.apply(statistik)));
        JFreeChart chart = ChartFactory.createPieChart(titel, data);
        return chart.createBufferedImage(600, 600);
    }

    public BufferedImage generateWeekBars() {
        DefaultCategoryDataset data = new DefaultCategoryDataset();
        Nutzerverwaltung n = UniqueBot.unique().nutzerverwaltung;
        for (Statistik statistik : statistikSet) {
            data.addValue(statistik.getMonday(), n.getBenutzer(statistik.getUserId()).getNutzername(), "Montag");
            data.addValue(statistik.getTuesday(), n.getBenutzer(statistik.getUserId()).getNutzername(), "Dienstag");
            data.addValue(statistik.getWednesday(), n.getBenutzer(statistik.getUserId()).getNutzername(), "Mittwoch");
            data.addValue(statistik.getThursday(), n.getBenutzer(statistik.getUserId()).getNutzername(), "Donnerstag");
            data.addValue(statistik.getFriday(), n.getBenutzer(statistik.getUserId()).getNutzername(), "Freitag");
            data.addValue(statistik.getSaturaday(), n.getBenutzer(statistik.getUserId()).getNutzername(), "Samstag");
            data.addValue(statistik.getSunday(), n.getBenutzer(statistik.getUserId()).getNutzername(), "Sonntag");
        }
        JFreeChart chart = ChartFactory.createStackedBarChart("Wochenverteilung", "Tag", "Nachrichten", data);
        return chart.createBufferedImage(600, 600);
    }

    public BufferedImage generateTimeBars() {
        DefaultCategoryDataset data = new DefaultCategoryDataset();
        Nutzerverwaltung n = UniqueBot.unique().nutzerverwaltung;
        for (Statistik statistik : statistikSet) {
            data.addValue(statistik.getMorning(), n.getBenutzer(statistik.getUserId()).getNutzername(), "Morgens");
            data.addValue(statistik.getDay(), n.getBenutzer(statistik.getUserId()).getNutzername(), "Mittags");
            data.addValue(statistik.getEvening(), n.getBenutzer(statistik.getUserId()).getNutzername(), "Abends");
            data.addValue(statistik.getNight(), n.getBenutzer(statistik.getUserId()).getNutzername(), "Nachts");
        }
        JFreeChart chart = ChartFactory.createStackedBarChart("Tagesverteilung", "Tageszeit", "Nachrichten", data);
        return chart.createBufferedImage(600, 600);
    }

    @SuppressWarnings("Duplicates")
    public BufferedImage generateTestDiagram() {
        DefaultCategoryDataset data = new DefaultCategoryDataset();
        Nutzerverwaltung n = UniqueBot.unique().nutzerverwaltung;
        for (Statistik statistik : statistikSet) {
            data.addValue(Math.max(n.getBenutzer(statistik.getUserId()).getNextBelohnung() - System.currentTimeMillis(), 0) / 1000, "Cooldown", n.getBenutzer(statistik.getUserId()).getNutzername());
        }
        JFreeChart chart = ChartFactory.createBarChart("Zeit bis zu nächsten Belohnung", "Nutzer", "Sekunden", data, PlotOrientation.HORIZONTAL, false, true, false);
        return chart.createBufferedImage(600, 600);
    }
}
