import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by Eloa Franca Verona
 * This class generates a word cloud from an input text file and saves it as a png image to an output folder
 */
public class WordCloudMaker extends JPanel {
    private static final int CANVAS_MAX_WIDTH = 960;
    private static final int CANVAS_MAX_HEIGHT = 720;
    private static final FontRenderContext FRC = new FontRenderContext(null, true, true);
    private int CANVAS_HEIGHT = CANVAS_MAX_HEIGHT;
    private int CANVAS_WIDTH = CANVAS_MAX_WIDTH;
    private LinkedHashMap<String, Integer> wordCountOrdered;
    private ArrayList<Shape> finalWordShapes;
    private int spacingBetweenWords = 10;
    private boolean addExtraPaddingToWords = false;

    private WordCloudMaker(String filePath){
        WordFreqCounter wfq = new WordFreqCounter(filePath);
        wordCountOrdered = wfq.getWordCountOrdered();
    }

    /**
     * Calls methods to generate world cloud using random placement strategy
     */
    private void randomPlacementStrategy(){
        addExtraPaddingToWords = true;
        finalWordShapes = findPositionRandom(wordCountOrdered);
        setPreferredSize(new Dimension(CANVAS_WIDTH, CANVAS_HEIGHT));
    }

    /**
     * Calls methods to generate world cloud using linear unordered strategy
     */
    private void linearUnorderedPlacementStrategy(){
        addExtraPaddingToWords = true;
        finalWordShapes = findPositionLinearUnordered(wordCountOrdered);
        setPreferredSize(new Dimension(CANVAS_WIDTH, CANVAS_HEIGHT));
    }
    /**
     * Calls methods to generate world cloud using linear ordered strategy
     */
    private void linearOrderedPlacementStrategy(){
        finalWordShapes = findPositionLinearOrdered(wordCountOrdered);
        setPreferredSize(new Dimension(CANVAS_WIDTH, CANVAS_HEIGHT));
    }

    /**
     * Paint words to JPanel
     */

    public void paintComponent(Graphics g) {
        setOpaque(true);
        setBackground(Color.WHITE);
        super.paintComponent(g);  // paint background
        Graphics2D g2 = (Graphics2D) g;
        Color[] colors = generateColor(4);
        int index = 0;
        for (Shape word : finalWordShapes) {
            g2.setColor(colors[index]);
            if (index == colors.length - 1) index = 0;
            else index++;
            g2.fill(word);
            g2.draw(word);
        }
    }

    /**
     * Generates random pastel colors for the words,
     * @param numOfColors number of colors to be generated
     * @return
     */
    private Color[] generateColor(int numOfColors){
        Color[] colors = new Color[numOfColors];
        Random rand = new Random();
        for(int i = 0; i < numOfColors; i ++){
            float hue = rand.nextFloat();
            float saturation = (rand.nextInt(2000) + 4000) / 10000f;
            float luminance = 0.9f;
            Color color = Color.getHSBColor(hue, saturation, luminance);
            colors[i] = color;
        }
        return colors;
    }
    /**
     * Takes in  total area taken by the words to be placed and sets the height of the canvas to be as big as needed to fit the words
     *
     * @param totalArea
     */
    private void resizeCanvas(double totalArea) {
        int minHeight = 130;
        CANVAS_HEIGHT = Math.max(minHeight, (int) (Math.ceil(totalArea / CANVAS_MAX_WIDTH)));
    }
    /**
     * This method calculates that a word shape takes and adds it to the area other words already placed take
     * the actual area all words take when placed in the canvas is actually bigger than the simple sum of
     *the words areas. Depending on the placement strategy, extra space will be added to the height of the word.
     * to account for extra space they may take.
     * to account for that
     * @param word
     * @param areaSoFar
     * @return
     */
    private double calculateArea(Shape word, double areaSoFar) {
        double extraPaddingToWords = 0;
        if(addExtraPaddingToWords) {
            extraPaddingToWords = word.getBounds2D().getHeight() * 0.45;
        }
        return areaSoFar + ((word.getBounds2D().getHeight() + spacingBetweenWords + extraPaddingToWords) * (word.getBounds2D().getWidth() + spacingBetweenWords));
    }


    /**
     * This goes through a Map of words and their frequency count and create a shape for each word
     * if the area the words take gets bigger than that maximum area of the canvas it stops going through words in the Map
     * If the are the words take is smaller thant the maximum area of the canvas it calls resizeCanvas to set a new height for the canvas
     *
     * @param wordCount
     * @return a ArrayList with the Shapes of each word to be placed in the canvas
     */
    private ArrayList<Shape> getAllWordsShape(Map<String, Integer> wordCount) {
        double maxCount = 0;
        Iterator<String> it = wordCount.keySet().iterator();
        ArrayList<Shape> wordShapes = new ArrayList<>();
        double totalArea = 0;
        boolean isSmallerThanCanvas = true;
        while (it.hasNext()) {
            String wordStr = it.next();
            int size = wordCount.get(wordStr);
            if (maxCount == 0) {
                maxCount = size;
            }
            Shape word = generateWordsShape(wordStr, size, maxCount);
            totalArea = calculateArea(word, totalArea);
            if (totalArea >= (CANVAS_MAX_HEIGHT * CANVAS_MAX_WIDTH)) {
                isSmallerThanCanvas = false;
                break;
            }
            wordShapes.add(word);
        }
        if (isSmallerThanCanvas) {
            resizeCanvas(totalArea);
        }
        return wordShapes;
    }

    /**
     * This method takes in a string word and a word count and creates the shape of the word with size on the count
     *
     * @param word
     * @param wordCount
     * @param maxCount  the size of every word depends on the size of the word with the biggest count.
     * @return
     */
    public Shape generateWordsShape(String word, int wordCount, double maxCount) {
        double sizeRatio = 70;
        double minSize = 10;
        double normSize = ((wordCount / maxCount) * sizeRatio) + minSize;
        Font font = new Font("Monospaced", Font.PLAIN, (int) normSize);
        Shape wordShape = generate(font, normSize, word);
        return wordShape;
    }

    /**
     * This method is calculates the position of every word to be placed.
     * The placement strategy is to place every word side by side,
     * ordered from most common word to least common word
     *
     * @param wordCount
     * @return
     */
    public ArrayList<Shape> findPositionLinearOrdered(Map<String, Integer> wordCount) {
        Rectangle2D lastBounds = null;
        Rectangle2D wordMaxHeight = null;
        ArrayList<Shape> transformedWords = new ArrayList<>();
        boolean startedNewLine = false;
        Iterator<String> it = wordCount.keySet().iterator();
        double maxCount = 0;
        double totalArea = 0;
        boolean isMaxSize = false;
        while (it.hasNext()) {
            String wordStr = it.next();
            int size = wordCount.get(wordStr);
            if (maxCount == 0) {
                maxCount = size;
            } //the first word is the one with maximum count
            Shape word = generateWordsShape(wordStr, size, maxCount);
            totalArea = calculateArea(word, totalArea);
            // translate first word
            if (transformedWords.isEmpty()) {
                Shape transformedWord = AffineTransform.getTranslateInstance(0, word.getBounds2D().getHeight()).createTransformedShape(word);
                lastBounds = transformedWord.getBounds2D();
                transformedWords.add(transformedWord);
                wordMaxHeight = transformedWord.getBounds2D();
            } //translate other words based on the position of the word that was positioned right before
            else {
                double newPosX = lastBounds.getX() + lastBounds.getWidth() + spacingBetweenWords;
                double newPosY = wordMaxHeight.getY() + wordMaxHeight.getHeight();
                if (newPosX + word.getBounds2D().getWidth() >= CANVAS_WIDTH) {
                    newPosY = newPosY + word.getBounds2D().getHeight() + spacingBetweenWords;
                    newPosX = 0;
                    startedNewLine = true;
                }
                if (newPosY + word.getBounds2D().getHeight() >= CANVAS_HEIGHT) {
                    isMaxSize = true;
                    break;
                }
                Shape transformedWord = AffineTransform.getTranslateInstance(newPosX, newPosY).createTransformedShape(word);
                lastBounds = transformedWord.getBounds2D();
                transformedWords.add(transformedWord);
                if (startedNewLine) {
                    wordMaxHeight = transformedWord.getBounds2D();
                    startedNewLine = false;
                }
            }
        }
        if (!isMaxSize) resizeCanvas(totalArea); //if words do not take all the space than resize canvas
        return transformedWords;
    }

    /**
     * This method is calculates the position of every word to be placed.
     * The placement strategy is to place every word side by side,
     * in random order
     *
     * @param wordCount
     * @return
     */
    public ArrayList<Shape> findPositionLinearUnordered(Map<String, Integer> wordCount) {
        Rectangle2D lastBounds = null;
        Rectangle2D wordMaxHeight = null;
        ArrayList<Shape> transformedWords = new ArrayList<>();
        ArrayList<Shape> shapesInThisLine = new ArrayList<>();
        boolean startedNewLine = false;
        boolean changedLineHeight = false;
        ArrayList<Shape> wordShapes = getAllWordsShape(wordCount);
        Collections.shuffle(wordShapes);
        for (Shape word : wordShapes) {
            //first word to be placed
            if (lastBounds == null) {
                Shape transformedWord = AffineTransform.getTranslateInstance(0, word.getBounds2D().getHeight()).createTransformedShape(word);
                lastBounds = transformedWord.getBounds2D();
                wordMaxHeight = transformedWord.getBounds2D();
                shapesInThisLine.add(transformedWord);
            } else {
                double newPosX = lastBounds.getX() + lastBounds.getWidth() + spacingBetweenWords;
                double newPosY = wordMaxHeight.getY() + wordMaxHeight.getHeight();
                //start a new line
                if (newPosX + word.getBounds2D().getWidth() >= CANVAS_WIDTH) {
                    newPosY = newPosY + word.getBounds2D().getHeight() + spacingBetweenWords;
                    newPosX = 0;
                    startedNewLine = true;
                    transformedWords.addAll(shapesInThisLine);
                    shapesInThisLine.clear();
                }
                //word to be placed in line is taller than other words in the line
                if (word.getBounds2D().getHeight() > wordMaxHeight.getHeight() && !startedNewLine) {
                    changedLineHeight = true;
                    newPosY = newPosY + (word.getBounds2D().getHeight() - wordMaxHeight.getHeight());
                    ArrayList<Shape> temp = new ArrayList<>();
                    for (Shape wordInLine : shapesInThisLine) {
                        double newPosYInLine = word.getBounds2D().getHeight() - wordMaxHeight.getHeight();
                        Shape newWordInLine = AffineTransform.getTranslateInstance(0, newPosYInLine).createTransformedShape(wordInLine);
                        temp.add(newWordInLine);
                    }
                    shapesInThisLine.clear();
                    shapesInThisLine.addAll(temp);
                    temp.clear();
                }
                Shape transformedWord = AffineTransform.getTranslateInstance(newPosX, newPosY).createTransformedShape(word);
                lastBounds = transformedWord.getBounds2D();
                shapesInThisLine.add(transformedWord);
                if (startedNewLine) {
                    wordMaxHeight = transformedWord.getBounds2D();
                    startedNewLine = false;
                }
                if (changedLineHeight) {
                    wordMaxHeight = transformedWord.getBounds2D();
                    changedLineHeight = false;
                }
            }
        }
        transformedWords.addAll(shapesInThisLine);
        return transformedWords;
    }

    /**
     * This method is calculates the position of every word to be placed.
     * The placement strategy is to place every word in a random spot in the canvas
     *
     * @param wordCount
     * @return
     */

    public ArrayList<Shape> findPositionRandom(Map<String, Integer> wordCount) {
        ArrayList<Shape> transformedWords = new ArrayList<>();
        ArrayList<Shape> wordShapes = getAllWordsShape(wordCount);
        Random rgWidth = new Random();
        Random rgHeight = new Random();
        int[][] grid = new int[CANVAS_HEIGHT][CANVAS_WIDTH];
        for (Shape word : wordShapes) {
            int pixelsVisited = 0;
            int tempX = 0;
            int tempY = 0;
            boolean placed = false;
            ArrayList<Integer[]> visitedXY = new ArrayList<>();
            int wordWidth = (int) Math.ceil(word.getBounds().getWidth());
            int wordHeight = (int) Math.ceil(word.getBounds().getHeight());
            while (!placed) {
                //ramdomly select a position in the canvas to place word
                tempY = rgHeight.nextInt((CANVAS_HEIGHT - (wordHeight))) + wordHeight;
                tempX = rgWidth.nextInt(CANVAS_WIDTH - wordWidth);
                boolean brokeinnerloop = false;
                for (int i = tempY; i > tempY - (wordHeight); i--) {
                    for (int j = tempX; j < tempX + wordWidth; j++) {
                        if (grid[i][j] == 1) {
                            pixelsVisited++;
                            brokeinnerloop = true;
                            visitedXY.clear();
                            break;
                        } else {
                            Integer visited[] = {i, j};
                            visitedXY.add(visited);
                        }
                    }
                    if (brokeinnerloop) break;
                }
                if(pixelsVisited >= CANVAS_HEIGHT*CANVAS_WIDTH*0.99){
                    //If the words were placed in such a way that no more words fit,
                    // resize canvas
                    resizeCanvas(CANVAS_HEIGHT*CANVAS_WIDTH*1.5);
                    int[][] oldGrid = grid;
                    grid = new int[CANVAS_HEIGHT][CANVAS_WIDTH];
                    System.arraycopy(oldGrid, 0, grid, 0, oldGrid.length);
                }
                if (!brokeinnerloop) placed = true;
            }
            for (int i = 0; i < visitedXY.size(); i++) {
                //mark the places where the word was placed with a 1
                Integer visited[] = visitedXY.get(i);
                grid[visited[0]][visited[1]] = 1;
            }
            Shape transformedWord = AffineTransform.getTranslateInstance(tempX, tempY).createTransformedShape(word);
            transformedWords.add(transformedWord);
        }
        return transformedWords;
    }


    /**
     * This method takes in a string and transform it in a shape object. It was adapted from the chapter book by Jonathan Feinberg.
     * Retrieved from "http://static.mrfeinberg.com/bv_ch03.pdf"
     *
     * @param font
     * @param weight
     * @param word
     * @return
     */
    public Shape generate(final Font font, final double weight, final String word) {
        final Font sizedFont = font.deriveFont((float) weight);
        final char[] chars = word.toCharArray();
        final GlyphVector gv = sizedFont.layoutGlyphVector(FRC, chars, 0, chars.length, 0);
        return gv.getOutline(0, 0);
    }


    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Invalid number of arguments. Please provide type of word cloud (linear-ordered," +
                    "linear-unordered or random), path " +
                    "to input text file and path to the directory the program will save the output image ");
            System.exit(1);
        }
        String cloudType = args[0];
        String inputFile = args[1];
        String outputDir = args[2];
        boolean inputFileIsValid = isInputFileValid(inputFile);
        if (!inputFileIsValid) {
            System.out.println("Error reading input file, please make sure the path and file type are" +
                    " correct. Files must be in text format (txt), and must not be empty.");
            System.exit(1);
        }
        boolean outputDirExists = new File(outputDir).isDirectory();
        if (!outputDirExists) {
            System.out.println("Output directory does not exists. Please make sure the path is correct.");
            System.exit(1);
        }

        WordCloudMaker wc = new WordCloudMaker(inputFile);
        switch (cloudType) {
            case "linear-ordered":
                wc.linearOrderedPlacementStrategy();
                break;
            case "linear-unordered":
                wc.linearUnorderedPlacementStrategy();
                break;
            case "random":
                wc.randomPlacementStrategy();
                break;
            default:
                System.out.println("Invalid Word Cloud type. The three valid types are: linear-ordered, linear-unordered or random");
                System.exit(1);
        }

        generateImage(outputDir, wc);
    }

    private static String generatePathWithFileName(String outputDir, String fileName){
        if(outputDir.lastIndexOf('/') != outputDir.length()-1){
            outputDir = outputDir + "/";
        }
        String filePath = outputDir + "/" + fileName;
        String[] fileNameParts = fileName.split("\\.");
        File temp = new File(filePath);
        int i = 1;
        while (temp.exists()) {
            i++;
            filePath = outputDir + fileNameParts[0] +"_" + i + "." + fileNameParts[1];
            temp = new File(filePath);
        }
        return filePath;
    }

    private static void generateImage(String fileOutput, WordCloudMaker wc) {
        JFrame frame = new JFrame();
        frame.setContentPane(wc);
        frame.pack();
        BufferedImage bi = new BufferedImage(wc.getWidth(), wc.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = bi.createGraphics();
        wc.print(graphics);
        graphics.dispose();
        frame.dispose();
        String filePath = generatePathWithFileName(fileOutput, "word_cloud.png");
        try {
            File outputfile = new File(filePath);
            ImageIO.write(bi, "png", outputfile);
        } catch (IOException e) {
            System.out.println("Error in writing image to file. " + e);
            System.exit(1);
        }
    }
    private static boolean isInputFileValid(String fileInput) {
        File inputFile = new File(fileInput);
        boolean fileIsEmpty = false;
        if(inputFile.length()==0) fileIsEmpty = true;
        int i = fileInput.lastIndexOf('.');
        String extension = "";
        if (i >= 0) {
            extension = fileInput.substring(i + 1);
        }
        return inputFile.isFile() && extension.equals("txt") && !fileIsEmpty;
    }
}
