import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

public class SF6StatsTracker {

	private static final String FILE_NAME = "sf6_history.txt";
	private static final String CHAR_FILE = "sf6_characters.txt";
	private static final Charset WINDOWS_CHARSET = Charset.forName("MS932");

	private static List<MatchResult> history = new ArrayList<>();
	private static List<String> charList = new ArrayList<>();
	private static JTextArea txtAreaStats;
	private static JComboBox<String> dateFilterBox;

	// ============================
	// MatchResult（スト6用）
	// ============================
	public static class MatchResult {
		private final String myCharacter;
		private final String enemyCharacter;
		private final int result; // 勝ち=1 / 負け=0
		private final int masterRate; // マスター帯
		private final String date;

		public MatchResult(String myCharacter, String enemyCharacter, int result, int masterRate, String date) {
			this.myCharacter = myCharacter;
			this.enemyCharacter = enemyCharacter;
			this.result = result;
			this.masterRate = masterRate;
			this.date = date;
		}

		public String getMyCharacter() {
			return myCharacter;
		}

		public String getEnemyCharacter() {
			return enemyCharacter;
		}

		public int getResult() {
			return result;
		}

		public int getMasterRate() {
			return masterRate;
		}

		public String getDate() {
			return date;
		}
	}

	// ============================
	// キャラ読み込み
	// ============================
	private static void loadCharacters() {
		Path path = Path.of(CHAR_FILE);

		if (!Files.exists(path)) {
			charList.addAll(List.of(
					"リュウ", "ケン", "ルーク", "ジュリ", "キャミィ",
					"春麗", "マリーザ", "ザンギエフ", "ガイル", "ダルシム"));
			saveCharacters();
			return;
		}

		try (BufferedReader br = Files.newBufferedReader(path, WINDOWS_CHARSET)) {
			String line;
			while ((line = br.readLine()) != null) {
				if (!line.isBlank())
					charList.add(line.trim());
			}
		} catch (IOException e) {
			System.out.println("キャラ読み込みエラー: " + e.getMessage());
		}
	}

	private static void saveCharacters() {
		try (BufferedWriter bw = Files.newBufferedWriter(Path.of(CHAR_FILE), WINDOWS_CHARSET)) {
			for (String c : charList) {
				bw.write(c);
				bw.newLine();
			}
		} catch (IOException e) {
			System.out.println("キャラ保存エラー: " + e.getMessage());
		}
	}

	// ============================
	// データ読み込み
	// ============================
	private static void loadData() {
		Path path = Path.of(FILE_NAME);
		if (!Files.exists(path))
			return;

		try (BufferedReader br = Files.newBufferedReader(path, WINDOWS_CHARSET)) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] d = line.split(",");
				if (d.length == 5) {
					history.add(new MatchResult(
							d[0], d[1],
							Integer.parseInt(d[2]),
							Integer.parseInt(d[3]),
							d[4]));
				}
			}
		} catch (Exception e) {
			System.out.println("データ読み込みエラー: " + e.getMessage());
		}
	}

	// ============================
	// データ保存
	// ============================
	private static void saveData() {
		try (BufferedWriter bw = Files.newBufferedWriter(Path.of(FILE_NAME), WINDOWS_CHARSET)) {
			for (MatchResult m : history) {
				bw.write(
						m.getMyCharacter() + "," +
								m.getEnemyCharacter() + "," +
								m.getResult() + "," +
								m.getMasterRate() + "," +
								m.getDate());
				bw.newLine();
			}
		} catch (IOException e) {
			System.out.println("保存エラー: " + e.getMessage());
		}
	}

	// ============================
	// GUI
	// ============================
	public static void main(String[] args) {
		loadCharacters();
		loadData();
		SwingUtilities.invokeLater(SF6StatsTracker::createGUI);
	}

	private static void createGUI() {

		JFrame frame = new JFrame("スト6戦績管理");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(900, 650);
		frame.setLayout(new BorderLayout());

		// 入力パネル
		JPanel inputPanel = new JPanel(new GridLayout(4, 2, 5, 5));
		inputPanel.setBorder(BorderFactory.createTitledBorder("試合データ入力"));

		JComboBox<String> cmbMyChar = new JComboBox<>(charList.toArray(new String[0]));
		JComboBox<String> cmbEnemyChar = new JComboBox<>(charList.toArray(new String[0]));
		JComboBox<String> cmbResult = new JComboBox<>(new String[] { "勝ち", "負け" });
		JTextArea txtMasterRate = new JTextArea("1200");

		inputPanel.add(new JLabel("自キャラ:"));
		inputPanel.add(cmbMyChar);
		inputPanel.add(new JLabel("相手キャラ:"));
		inputPanel.add(cmbEnemyChar);
		inputPanel.add(new JLabel("勝敗:"));
		inputPanel.add(cmbResult);
		inputPanel.add(new JLabel("マスター帯:"));
		inputPanel.add(txtMasterRate);

		// ボタン
		JPanel buttonPanel = new JPanel(new FlowLayout());
		JButton btnRegister = new JButton("登録");
		JButton btnReset = new JButton("全削除");
		JButton btnAddChar = new JButton("キャラ追加");
		JButton btnDeleteChar = new JButton("キャラ削除");

		buttonPanel.add(btnRegister);
		buttonPanel.add(btnReset);
		buttonPanel.add(btnAddChar);
		buttonPanel.add(btnDeleteChar);

		// 日付フィルタ
		JPanel filterPanel = new JPanel(new FlowLayout());
		dateFilterBox = new JComboBox<>();
		JButton btnShowAll = new JButton("全体表示");

		filterPanel.add(new JLabel("日付フィルタ:"));
		filterPanel.add(dateFilterBox);
		filterPanel.add(btnShowAll);

		// 上部まとめ
		JPanel topPanel = new JPanel(new BorderLayout());
		topPanel.add(inputPanel, BorderLayout.CENTER);
		topPanel.add(buttonPanel, BorderLayout.SOUTH);
		topPanel.add(filterPanel, BorderLayout.NORTH);

		frame.add(topPanel, BorderLayout.NORTH);

		// 戦績表示
		txtAreaStats = new JTextArea();
		txtAreaStats.setEditable(false);
		JScrollPane scrollPane = new JScrollPane(txtAreaStats);

		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab("戦績一覧", scrollPane);
		tabs.addTab("日付勝率グラフ", createDailyGraph());
		tabs.addTab("日付MR推移", createDailyMRGraph());

		frame.add(tabs, BorderLayout.CENTER);

		// ============================
		// イベント
		// ============================

		btnRegister.addActionListener(e -> {
			String my = cmbMyChar.getSelectedItem().toString();
			String enemy = cmbEnemyChar.getSelectedItem().toString();
			int result = cmbResult.getSelectedItem().equals("勝ち") ? 1 : 0;
			int master = Integer.parseInt(txtMasterRate.getText());
			String date = java.time.LocalDate.now().toString();

			history.add(new MatchResult(my, enemy, result, master, date));
			saveData();
			refreshDateFilter();
			tabs.setComponentAt(1, createDailyGraph());
			tabs.setComponentAt(2, createDailyMRGraph());

			updateStatsDisplayAll();

			JOptionPane.showMessageDialog(frame, "登録しました！");
		});

		btnReset.addActionListener(e -> {
			if (JOptionPane.showConfirmDialog(frame, "全データを削除しますか？") == JOptionPane.YES_OPTION) {
				history.clear();
				try {
					Files.deleteIfExists(Path.of(FILE_NAME));
				} catch (Exception ex) {
				}
				updateStatsDisplayAll();
			}
		});

		btnAddChar.addActionListener(e -> {
			String newChar = JOptionPane.showInputDialog("追加するキャラ名:");
			if (newChar != null && !newChar.isBlank()) {
				charList.add(newChar);
				cmbMyChar.addItem(newChar);
				cmbEnemyChar.addItem(newChar);
				saveCharacters();
			}
		});

		btnDeleteChar.addActionListener(e -> {
			String target = JOptionPane.showInputDialog("削除するキャラ名:");
			if (target != null && charList.remove(target)) {
				cmbMyChar.removeItem(target);
				cmbEnemyChar.removeItem(target);
				saveCharacters();
			}
		});

		dateFilterBox.addActionListener(e -> {
			String d = (String) dateFilterBox.getSelectedItem();
			updateStatsDisplay(d);
		});

		btnShowAll.addActionListener(e -> updateStatsDisplayAll());

		frame.setVisible(true);
		refreshDateFilter();
		updateStatsDisplayAll();
	}

	// ============================
	// 戦績表示（スト6版）
	// ============================
	private static void updateStatsDisplayAll() {
		if (history.isEmpty()) {
			txtAreaStats.setText("データなし");
			return;
		}

		StringBuilder sb = new StringBuilder();
		sb.append("===== 全体戦績 =====\n");

		int total = history.size();
		int wins = 0;

		Map<String, Integer> charWins = new HashMap<>();
		Map<String, Integer> charTotal = new HashMap<>();

		for (MatchResult m : history) {
			if (m.getResult() == 1)
				wins++;

			charTotal.put(m.getMyCharacter(), charTotal.getOrDefault(m.getMyCharacter(), 0) + 1);
			if (m.getResult() == 1)
				charWins.put(m.getMyCharacter(), charWins.getOrDefault(m.getMyCharacter(), 0) + 1);
		}

		sb.append("総試合数: ").append(total).append("\n");
		sb.append("総勝率: ").append(String.format("%.1f", (double) wins / total * 100)).append("%\n\n");

		sb.append("===== キャラ別勝率 =====\n");
		for (String c : charTotal.keySet()) {
			int t = charTotal.get(c);
			int w = charWins.getOrDefault(c, 0);
			sb.append(c).append(": ").append(String.format("%.1f", (double) w / t * 100)).append("% (")
					.append(w).append("/").append(t).append(")\n");
		}

		txtAreaStats.setText(sb.toString());
	}

	private static void updateStatsDisplay(String date) {
		if (date == null)
			return;
		List<MatchResult> list = new ArrayList<>();

		for (MatchResult m : history)
			if (m.getDate().equals(date))
				list.add(m);

		if (list.isEmpty()) {
			txtAreaStats.setText(date + " のデータなし");
			return;
		}

		int total = list.size();
		int wins = 0;

		for (MatchResult m : list)
			if (m.getResult() == 1)
				wins++;

		txtAreaStats.setText(
				"===== " + date + " の戦績 =====\n" +
						"試合数: " + total + "\n" +
						"勝率: " + String.format("%.1f", (double) wins / total * 100) + "%\n");
	}

	// ============================
	// 日付勝率グラフ
	// ============================
	private static JPanel createDailyGraph() {
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		Map<String, List<MatchResult>> map = new HashMap<>();
		for (MatchResult m : history) {
			map.putIfAbsent(m.getDate(), new ArrayList<>());
			map.get(m.getDate()).add(m);
		}

		List<String> dates = new ArrayList<>(map.keySet());
		dates.sort(String::compareTo);

		for (String d : dates) {
			List<MatchResult> list = map.get(d);
			int total = list.size();
			int wins = 0;
			for (MatchResult m : list)
				if (m.getResult() == 1)
					wins++;

			double rate = (double) wins / total * 100;
			dataset.addValue(rate, "勝率", d);
		}

		JFreeChart chart = ChartFactory.createLineChart(
				"日付別勝率",
				"日付",
				"勝率 (%)",
				dataset);

		// 1. 日本語対応フォントを用意
		Font commonFont = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
		Font titleFont = new Font(Font.SANS_SERIF, Font.BOLD, 18);

		// 2. タイトルのフォント設定
		if (chart.getTitle() != null) {
			chart.getTitle().setFont(titleFont);
		}

		// 3. 凡例（グラフ下部の「□□」部分）のフォント設定
		if (chart.getLegend() != null) {
			chart.getLegend().setItemFont(commonFont);
		}

		// 4. プロットエリア（軸など）のフォント設定 【ここを CategoryPlot に修正しました】
		CategoryPlot plot = chart.getCategoryPlot();
		
		if (plot != null) {
			// 縦軸（Y軸：数値軸）の設定
			if (plot.getRangeAxis() != null) {
				plot.getRangeAxis().setLabelFont(commonFont); // 「MR」のラベル部分
				plot.getRangeAxis().setTickLabelFont(commonFont); // 「1,200」などの数字部分
			}

			// 横軸（X軸：日付などのカテゴリ軸）の設定
			if (plot.getDomainAxis() != null) {
				plot.getDomainAxis().setLabelFont(commonFont); // 軸名ラベル部分
				// 縦軸ラベルの文字列を水平（横書き）にする
				plot.getRangeAxis().setLabelAngle(Math.PI / 2.0); 
				plot.getDomainAxis().setTickLabelFont(commonFont); // 「2026-07-24」などの項目部分
			}
		}
		

		LineAndShapeRenderer r = (LineAndShapeRenderer) plot.getRenderer();
		r.setSeriesStroke(0, new java.awt.BasicStroke(3.0f));
		r.setSeriesShapesVisible(0, true);

		return new ChartPanel(chart);
	}

	// ============================
	// 日付別 MR 推移グラフ
	// ============================
	private static JPanel createDailyMRGraph() {

		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		Map<String, List<MatchResult>> dateStats = new HashMap<>();
		for (MatchResult m : history) {
			dateStats.putIfAbsent(m.getDate(), new ArrayList<>());
			dateStats.get(m.getDate()).add(m);
		}

		List<String> sortedDates = new ArrayList<>(dateStats.keySet());
		sortedDates.sort(String::compareTo);

		for (String date : sortedDates) {
			List<MatchResult> list = dateStats.get(date);

			double totalMR = 0;
			for (MatchResult m : list)
				totalMR += m.getMasterRate();

			double avgMR = totalMR / list.size();

			dataset.addValue(avgMR, "平均MR", date);
		}

		JFreeChart chart = ChartFactory.createLineChart(
				"日付別 MR 推移",
				"日付",
				"",
				dataset);
		// 1. 日本語対応フォントを用意
		Font commonFont = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
		Font titleFont = new Font(Font.SANS_SERIF, Font.BOLD, 18);

		// ★【新設】縦軸ラベル専用に、最初から90度右に寝かせたフォントを作る
		java.awt.geom.AffineTransform transform = new java.awt.geom.AffineTransform();
		transform.rotate(Math.toRadians(90)); // 90度回転
		Font rotatedFont = commonFont.deriveFont(transform);
		
		// 2. タイトルのフォント設定
		if (chart.getTitle() != null) {
			chart.getTitle().setFont(titleFont);
		}

		// 3. 凡例（グラフ下部の「□□」部分）のフォント設定
		if (chart.getLegend() != null) {
			chart.getLegend().setItemFont(commonFont);
		}

		// 4. プロットエリア（軸など）のフォント設定
		CategoryPlot plot = chart.getCategoryPlot();
		if (plot != null) {
		    // 縦軸（Y軸：数値軸）の設定
		    if (plot.getRangeAxis() != null) {
		    	// ★ ラベル部分だけ、90度回転させたフォントをセットする
		        plot.getRangeAxis().setLabelFont(rotatedFont); 
		     // 目盛り（数字）は普通のフォントでOK
		        plot.getRangeAxis().setTickLabelFont(commonFont); 
		        plot.getRangeAxis().setLabelInsets(new org.jfree.ui.RectangleInsets(0, 0, 0, 15));
		    }
		    
		    
		    
		    // 横軸（X軸：日付などのカテゴリ軸）の設定
		    if (plot.getDomainAxis() != null) {
		        plot.getDomainAxis().setLabelFont(commonFont);
		        plot.getDomainAxis().setTickLabelFont(commonFont);
		    }
		    
		}



		// 縦軸ラベルの文字列を垂直（縦書き）にする
		plot.getRangeAxis().setLabelAngle(0.0);

		LineAndShapeRenderer r = (LineAndShapeRenderer) plot.getRenderer();
		r.setSeriesStroke(0, new java.awt.BasicStroke(3.0f));
		r.setSeriesShapesVisible(0, true);

		// ★ これを追加すると四角が消える
		chart.removeLegend();

		return new ChartPanel(chart);
	}

	private static void refreshDateFilter() {
		dateFilterBox.removeAllItems();
		Set<String> dates = new HashSet<>();
		for (MatchResult m : history)
			dates.add(m.getDate());

		List<String> sorted = new ArrayList<>(dates);
		sorted.sort(String::compareTo);

		for (String d : sorted)
			dateFilterBox.addItem(d);
	}
}
