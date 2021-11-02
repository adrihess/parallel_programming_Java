package com.company;

import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.function.IntBinaryOperator;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class MatrixRowSums {

    private static final int ROWS = 10;
    private static final int COLUMNS = 100;
    private static final int[] Sum= new int[COLUMNS];
    private static final AtomicIntegerArray List = new AtomicIntegerArray(ROWS);
    private static int akt;
    private static final CyclicBarrier barrier = new CyclicBarrier(COLUMNS, MatrixRowSums::print_out);


    private static void print_out() {
        int sum=0;
        for(int i=0; i<COLUMNS; i++)
            sum+= Sum[i];
        System.out.println(akt + " -> " +sum);
    }


    private static class Matrix {

        private final int rows;
        private final int columns;
        private final IntBinaryOperator definition;


        public Matrix(int rows, int columns, IntBinaryOperator definition) {
            this.rows = rows;
            this.columns = columns;
            this.definition = definition;
        }

        public int[] rowSums() {
            int[] rowSums = new int[rows];
            for (int row = 0; row < rows; ++row) {
                int sum = 0;
                for (int column = 0; column < columns; ++column) {
                    sum += definition.applyAsInt(row, column);
                }
                rowSums[row] = sum;
            }
            return rowSums;
        }
        private static class Helper implements Runnable{
            private final int column;
            private final int row;
            private final IntBinaryOperator definition;

            Helper(int column, int row, IntBinaryOperator definition) {
                this.row = row;
                this.column = column;
                this.definition=definition;

            }
            @Override
            public void run() {
                Sum[column] = definition.applyAsInt(row, column);
                try {
                    barrier.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    e.printStackTrace();
                }
            }
        }

        private static class HelperAtomicInt implements Runnable{
            private final int column;

            private final IntBinaryOperator definition;

            HelperAtomicInt(int column,  IntBinaryOperator definition) {
                this.column = column;
                this.definition=definition;
            }

            @Override
            public void run() {
                for(int i=0; i<ROWS; i++) {
                    int tempRes = definition.applyAsInt(i, column);
                    List.addAndGet(i, tempRes);
                }
            }
        }

        public int[] rowSumsConcurrent() throws InterruptedException {
            Thread[] watki = new Thread[COLUMNS];

            for(int i=0; i<rows; i++) {
                MatrixRowSums.akt = i;
                for(int j=0; j<columns; j++) {
                    watki[j] = new Thread(new Helper(j,i, definition));
                }

                for(int j=0; j<columns; j++){
                    watki[j].start();
                }
                for(int j=0; j<columns; j++){
                    watki[j].join();
                }
            }
            return null;
        }

        public void MatrixRowSumsThreadsafe() throws InterruptedException {
            Thread[] watki = new Thread[COLUMNS];

            for(int i=0; i<columns; i++) {
                watki[i] = new Thread(new HelperAtomicInt(i, definition));
                watki[i].start();
            }
            for(int i=0; i<columns; i++)
                watki[i].join();

            for(int i=0; i<rows; i++)
                System.out.println(i + " -> " + List.get(i));

        }
    }



    public static void main(String[] args) throws InterruptedException {
        Matrix matrix = new Matrix(ROWS, COLUMNS, (row, column) -> {
            int a = 2 * column + 1;
            return (row + 1) * (a % 4 - 2) * a;
        });

        int[] rowSums = matrix.rowSums();

        for (int i = 0; i < rowSums.length; i++) {
            System.out.println(i + " -> " + rowSums[i]);
        }
        System.out.println("Różnica");
        int[] rowSumsConc = matrix.rowSumsConcurrent();
        System.out.println("Różnica F");
        matrix.MatrixRowSumsThreadsafe();

    }

}
