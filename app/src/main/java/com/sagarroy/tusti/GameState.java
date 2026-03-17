package com.sagarroy.tusti;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameState {

    public static final int TUBE_CAPACITY = 4;
    public static final int TOTAL_LEVELS  = 25;

    private static int colorCountForLevel(int lvl) {
        if (lvl <= 5)  return 3;
        if (lvl <= 10) return 4;
        if (lvl <= 15) return 5;
        if (lvl <= 20) return 6;
        return 8;
    }

    public static String difficultyLabel(int lvl) {
        if (lvl <= 5)  return "EASY";
        if (lvl <= 10) return "MEDIUM";
        if (lvl <= 15) return "HARD";
        if (lvl <= 20) return "VERY HARD";
        return "IMPOSSIBLE";
    }

    public static int difficultyColor(int lvl) {
        if (lvl <= 5)  return 0xFF4CAF50;
        if (lvl <= 10) return 0xFF2196F3;
        if (lvl <= 15) return 0xFFFF9800;
        if (lvl <= 20) return 0xFFE53935;
        return 0xFF9C27B0;
    }

    public static final int[] WATER_COLORS = {
            0xFFE53935, 0xFF1E88E5, 0xFFFDD835, 0xFF43A047,
            0xFF8E24AA, 0xFF26C6DA, 0xFFEF6C00, 0xFFD81B60,
            0xFF6D4C41, 0xFF00BFA5,
    };

    public int  currentLevel = 1;
    private int colorCount;
    private int tubeCount;
    private int[][] tubes;
    private int[][] initialState;
    public  int  moves = 0;
    public  boolean won = false;

    public int getColorCount() { return colorCount; }
    public int getTubeCount()  { return tubeCount; }
    public int[][] getTubes()  { return tubes; }

    public void initLevel(int levelNumber) {
        currentLevel = levelNumber;
        colorCount   = colorCountForLevel(levelNumber);
        tubeCount    = colorCount + 2;
        moves = 0;
        won   = false;
        buildAndShuffle();
    }

    private void buildAndShuffle() {
        List<Integer> pool = new ArrayList<>();
        for (int c = 0; c < colorCount; c++)
            for (int k = 0; k < TUBE_CAPACITY; k++)
                pool.add(WATER_COLORS[c]);
        Collections.shuffle(pool);

        tubes = new int[tubeCount][TUBE_CAPACITY];
        int idx = 0;
        for (int i = 0; i < colorCount; i++)
            for (int j = 0; j < TUBE_CAPACITY; j++)
                tubes[i][j] = pool.get(idx++);
        for (int i = colorCount; i < tubeCount; i++)
            for (int j = 0; j < TUBE_CAPACITY; j++)
                tubes[i][j] = 0;
        initialState = copyTubes(tubes);
    }

    public void restart() {
        tubes = copyTubes(initialState);
        moves = 0;
        won   = false;
    }

    public void nextLevel() {
        initLevel((currentLevel % TOTAL_LEVELS) + 1);
    }

    public int topIndex(int tube) {
        for (int j = TUBE_CAPACITY - 1; j >= 0; j--)
            if (tubes[tube][j] != 0) return j;
        return -1;
    }

    public int topColor(int tube) {
        int idx = topIndex(tube);
        return idx == -1 ? 0 : tubes[tube][idx];
    }

    public int freeSpace(int tube) {
        int top = topIndex(tube);
        return top == -1 ? TUBE_CAPACITY : TUBE_CAPACITY - 1 - top;
    }

    public int topRun(int tube) {
        int top = topIndex(tube);
        if (top == -1) return 0;
        int color = tubes[tube][top];
        int count = 1;
        for (int j = top - 1; j >= 0; j--) {
            if (tubes[tube][j] == color) count++;
            else break;
        }
        return count;
    }

    public boolean canTransfer(int from, int to) {
        if (from == to) return false;
        if (topIndex(from) == -1) return false;
        if (freeSpace(to) == 0)   return false;
        int destTop = topIndex(to);
        if (destTop == -1) return true;
        return tubes[from][topIndex(from)] == tubes[to][destTop];
    }

    public int transfer(int from, int to) {
        if (!canTransfer(from, to)) return 0;
        int color   = topColor(from);
        int canMove = Math.min(topRun(from), freeSpace(to));
        for (int k = 0; k < canMove; k++) {
            tubes[from][topIndex(from)] = 0;
            tubes[to][topIndex(to) + 1] = color;
        }
        moves++;
        checkWon();
        return canMove;
    }

    private void checkWon() {
        for (int i = 0; i < tubeCount; i++) {
            int top = topIndex(i);
            if (top == -1) continue;
            if (top != TUBE_CAPACITY - 1) { won = false; return; }
            int c = tubes[i][0];
            for (int j = 1; j < TUBE_CAPACITY; j++)
                if (tubes[i][j] != c) { won = false; return; }
        }
        won = true;
    }

    private int[][] copyTubes(int[][] src) {
        int[][] dst = new int[src.length][TUBE_CAPACITY];
        for (int i = 0; i < src.length; i++)
            System.arraycopy(src[i], 0, dst[i], 0, TUBE_CAPACITY);
        return dst;
    }
}
