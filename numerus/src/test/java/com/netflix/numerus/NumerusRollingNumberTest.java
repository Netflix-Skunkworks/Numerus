package com.netflix.numerus;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.netflix.numerus.NumerusRollingNumber.Time;

public class NumerusRollingNumberTest {

    @Test
    public void testCreatesBuckets() {
        MockedTime time = new MockedTime();
        try {
            NumerusRollingNumber counter = new NumerusRollingNumber(time, 200, 10);
            // confirm the initial settings
            assertEquals(200, counter.timeInMilliseconds.get().intValue());
            assertEquals(10, counter.numberOfBuckets.get().intValue());
            assertEquals(20, counter.getBucketSizeInMilliseconds());

            // we start out with 0 buckets in the queue
            assertEquals(0, counter.buckets.size());

            // add a success in each interval which should result in all 10 buckets being created with 1 success in each
            for (int i = 0; i < counter.numberOfBuckets.get(); i++) {
                counter.increment(NumerusRollingNumberEvent.SUCCESS);
                time.increment(counter.getBucketSizeInMilliseconds());
            }

            // confirm we have all 10 buckets
            assertEquals(10, counter.buckets.size());

            // add 1 more and we should still only have 10 buckets since that's the max
            counter.increment(NumerusRollingNumberEvent.SUCCESS);
            assertEquals(10, counter.buckets.size());

        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception: " + e.getMessage());
        }
    }

    @Test
    public void testResetBuckets() {
        MockedTime time = new MockedTime();
        try {
            NumerusRollingNumber counter = new NumerusRollingNumber(time, 200, 10);

            // we start out with 0 buckets in the queue
            assertEquals(0, counter.buckets.size());

            // add 1
            counter.increment(NumerusRollingNumberEvent.SUCCESS);

            // confirm we have 1 bucket
            assertEquals(1, counter.buckets.size());

            // confirm we still have 1 bucket
            assertEquals(1, counter.buckets.size());

            // add 1
            counter.increment(NumerusRollingNumberEvent.SUCCESS);

            // we should now have a single bucket with no values in it instead of 2 or more buckets
            assertEquals(1, counter.buckets.size());

        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception: " + e.getMessage());
        }
    }

    @Test
    public void testEmptyBucketsFillIn() {
        MockedTime time = new MockedTime();
        try {
            NumerusRollingNumber counter = new NumerusRollingNumber(time, 200, 10);

            // add 1
            counter.increment(NumerusRollingNumberEvent.SUCCESS);

            // we should have 1 bucket
            assertEquals(1, counter.buckets.size());

            // wait past 3 bucket time periods (the 1st bucket then 2 empty ones)
            time.increment(counter.getBucketSizeInMilliseconds() * 3);

            // add another
            counter.increment(NumerusRollingNumberEvent.SUCCESS);

            // we should have 4 (1 + 2 empty + 1 new one) buckets
            assertEquals(4, counter.buckets.size());

        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception: " + e.getMessage());
        }
    }

    @Test
    public void testIncrementInSingleBucket() {
        MockedTime time = new MockedTime();
        try {
            NumerusRollingNumber counter = new NumerusRollingNumber(time, 200, 10);

            // increment
            counter.increment(NumerusRollingNumberEvent.SUCCESS);
            counter.increment(NumerusRollingNumberEvent.SUCCESS);
            counter.increment(NumerusRollingNumberEvent.SUCCESS);
            counter.increment(NumerusRollingNumberEvent.SUCCESS);
            counter.increment(NumerusRollingNumberEvent.FAILURE);
            counter.increment(NumerusRollingNumberEvent.FAILURE);
            counter.increment(NumerusRollingNumberEvent.TIMEOUT);

            // we should have 1 bucket
            assertEquals(1, counter.buckets.size());

            // the count should be 4
            assertEquals(4, counter.buckets.getLast().getAdder(NumerusRollingNumberEvent.SUCCESS).sum());
            assertEquals(2, counter.buckets.getLast().getAdder(NumerusRollingNumberEvent.FAILURE).sum());
            assertEquals(1, counter.buckets.getLast().getAdder(NumerusRollingNumberEvent.TIMEOUT).sum());

        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception: " + e.getMessage());
        }
    }

    @Test
    public void testTimeout() {
        MockedTime time = new MockedTime();
        try {
            NumerusRollingNumber counter = new NumerusRollingNumber(time, 200, 10);

            // increment
            counter.increment(NumerusRollingNumberEvent.TIMEOUT);

            // we should have 1 bucket
            assertEquals(1, counter.buckets.size());

            // the count should be 1
            assertEquals(1, counter.buckets.getLast().getAdder(NumerusRollingNumberEvent.TIMEOUT).sum());
            assertEquals(1, counter.getRollingSum(NumerusRollingNumberEvent.TIMEOUT));

            // sleep to get to a new bucket
            time.increment(counter.getBucketSizeInMilliseconds() * 3);

            // incremenet again in latest bucket
            counter.increment(NumerusRollingNumberEvent.TIMEOUT);

            // we should have 4 buckets
            assertEquals(4, counter.buckets.size());

            // the counts of the last bucket
            assertEquals(1, counter.buckets.getLast().getAdder(NumerusRollingNumberEvent.TIMEOUT).sum());

            // the total counts
            assertEquals(2, counter.getRollingSum(NumerusRollingNumberEvent.TIMEOUT));

        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception: " + e.getMessage());
        }
    }

    @Test
    public void testShortCircuited() {
        MockedTime time = new MockedTime();
        try {
            NumerusRollingNumber counter = new NumerusRollingNumber(time, 200, 10);

            // increment
            counter.increment(NumerusRollingNumberEvent.SHORT_CIRCUITED);

            // we should have 1 bucket
            assertEquals(1, counter.buckets.size());

            // the count should be 1
            assertEquals(1, counter.buckets.getLast().getAdder(NumerusRollingNumberEvent.SHORT_CIRCUITED).sum());
            assertEquals(1, counter.getRollingSum(NumerusRollingNumberEvent.SHORT_CIRCUITED));

            // sleep to get to a new bucket
            time.increment(counter.getBucketSizeInMilliseconds() * 3);

            // incremenet again in latest bucket
            counter.increment(NumerusRollingNumberEvent.SHORT_CIRCUITED);

            // we should have 4 buckets
            assertEquals(4, counter.buckets.size());

            // the counts of the last bucket
            assertEquals(1, counter.buckets.getLast().getAdder(NumerusRollingNumberEvent.SHORT_CIRCUITED).sum());

            // the total counts
            assertEquals(2, counter.getRollingSum(NumerusRollingNumberEvent.SHORT_CIRCUITED));

        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception: " + e.getMessage());
        }
    }

    @Test
    public void testThreadPoolRejection() {
        testCounterType(NumerusRollingNumberEvent.THREAD_POOL_REJECTED);
    }

    @Test
    public void testFallbackSuccess() {
        testCounterType(NumerusRollingNumberEvent.FALLBACK_SUCCESS);
    }

    @Test
    public void testFallbackFailure() {
        testCounterType(NumerusRollingNumberEvent.FALLBACK_FAILURE);
    }

    @Test
    public void testExceptionThrow() {
        testCounterType(NumerusRollingNumberEvent.EXCEPTION_THROWN);
    }

    private void testCounterType(NumerusRollingNumberEvent type) {
        MockedTime time = new MockedTime();
        try {
            NumerusRollingNumber counter = new NumerusRollingNumber(time, 200, 10);

            // increment
            counter.increment(type);

            // we should have 1 bucket
            assertEquals(1, counter.buckets.size());

            // the count should be 1
            assertEquals(1, counter.buckets.getLast().getAdder(type).sum());
            assertEquals(1, counter.getRollingSum(type));

            // sleep to get to a new bucket
            time.increment(counter.getBucketSizeInMilliseconds() * 3);

            // increment again in latest bucket
            counter.increment(type);

            // we should have 4 buckets
            assertEquals(4, counter.buckets.size());

            // the counts of the last bucket
            assertEquals(1, counter.buckets.getLast().getAdder(type).sum());

            // the total counts
            assertEquals(2, counter.getRollingSum(type));

        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception: " + e.getMessage());
        }
    }

    @Test
    public void testIncrementInMultipleBuckets() {
        MockedTime time = new MockedTime();
        try {
            NumerusRollingNumber counter = new NumerusRollingNumber(time, 200, 10);

            // increment
            counter.increment(NumerusRollingNumberEvent.SUCCESS);
            counter.increment(NumerusRollingNumberEvent.SUCCESS);
            counter.increment(NumerusRollingNumberEvent.SUCCESS);
            counter.increment(NumerusRollingNumberEvent.SUCCESS);
            counter.increment(NumerusRollingNumberEvent.FAILURE);
            counter.increment(NumerusRollingNumberEvent.FAILURE);
            counter.increment(NumerusRollingNumberEvent.TIMEOUT);
            counter.increment(NumerusRollingNumberEvent.TIMEOUT);
            counter.increment(NumerusRollingNumberEvent.SHORT_CIRCUITED);

            // sleep to get to a new bucket
            time.increment(counter.getBucketSizeInMilliseconds() * 3);

            // increment
            counter.increment(NumerusRollingNumberEvent.SUCCESS);
            counter.increment(NumerusRollingNumberEvent.SUCCESS);
            counter.increment(NumerusRollingNumberEvent.FAILURE);
            counter.increment(NumerusRollingNumberEvent.FAILURE);
            counter.increment(NumerusRollingNumberEvent.FAILURE);
            counter.increment(NumerusRollingNumberEvent.TIMEOUT);
            counter.increment(NumerusRollingNumberEvent.SHORT_CIRCUITED);

            // we should have 4 buckets
            assertEquals(4, counter.buckets.size());

            // the counts of the last bucket
            assertEquals(2, counter.buckets.getLast().getAdder(NumerusRollingNumberEvent.SUCCESS).sum());
            assertEquals(3, counter.buckets.getLast().getAdder(NumerusRollingNumberEvent.FAILURE).sum());
            assertEquals(1, counter.buckets.getLast().getAdder(NumerusRollingNumberEvent.TIMEOUT).sum());
            assertEquals(1, counter.buckets.getLast().getAdder(NumerusRollingNumberEvent.SHORT_CIRCUITED).sum());

            // the total counts
            assertEquals(6, counter.getRollingSum(NumerusRollingNumberEvent.SUCCESS));
            assertEquals(5, counter.getRollingSum(NumerusRollingNumberEvent.FAILURE));
            assertEquals(3, counter.getRollingSum(NumerusRollingNumberEvent.TIMEOUT));
            assertEquals(2, counter.getRollingSum(NumerusRollingNumberEvent.SHORT_CIRCUITED));

            // wait until window passes
            time.increment(counter.timeInMilliseconds.get());

            // increment
            counter.increment(NumerusRollingNumberEvent.SUCCESS);

            // the total counts should now include only the last bucket after a reset since the window passed
            assertEquals(1, counter.getRollingSum(NumerusRollingNumberEvent.SUCCESS));
            assertEquals(0, counter.getRollingSum(NumerusRollingNumberEvent.FAILURE));
            assertEquals(0, counter.getRollingSum(NumerusRollingNumberEvent.TIMEOUT));

        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception: " + e.getMessage());
        }
    }

    @Test
    public void testCounterRetrievalRefreshesBuckets() {
        MockedTime time = new MockedTime();
        try {
            NumerusRollingNumber counter = new NumerusRollingNumber(time, 200, 10);

            // increment
            counter.increment(NumerusRollingNumberEvent.SUCCESS);
            counter.increment(NumerusRollingNumberEvent.SUCCESS);
            counter.increment(NumerusRollingNumberEvent.SUCCESS);
            counter.increment(NumerusRollingNumberEvent.SUCCESS);
            counter.increment(NumerusRollingNumberEvent.FAILURE);
            counter.increment(NumerusRollingNumberEvent.FAILURE);

            // sleep to get to a new bucket
            time.increment(counter.getBucketSizeInMilliseconds() * 3);

            // we should have 1 bucket since nothing has triggered the update of buckets in the elapsed time
            assertEquals(1, counter.buckets.size());

            // the total counts
            assertEquals(4, counter.getRollingSum(NumerusRollingNumberEvent.SUCCESS));
            assertEquals(2, counter.getRollingSum(NumerusRollingNumberEvent.FAILURE));

            // we should have 4 buckets as the counter 'gets' should have triggered the buckets being created to fill in time
            assertEquals(4, counter.buckets.size());

            // wait until window passes
            time.increment(counter.timeInMilliseconds.get());

            // the total counts should all be 0 (and the buckets cleared by the get, not only increment)
            assertEquals(0, counter.getRollingSum(NumerusRollingNumberEvent.SUCCESS));
            assertEquals(0, counter.getRollingSum(NumerusRollingNumberEvent.FAILURE));

            // increment
            counter.increment(NumerusRollingNumberEvent.SUCCESS);

            // the total counts should now include only the last bucket after a reset since the window passed
            assertEquals(1, counter.getRollingSum(NumerusRollingNumberEvent.SUCCESS));
            assertEquals(0, counter.getRollingSum(NumerusRollingNumberEvent.FAILURE));

        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception: " + e.getMessage());
        }
    }

    @Test
    public void testUpdateMax1() {
        MockedTime time = new MockedTime();
        try {
            NumerusRollingNumber counter = new NumerusRollingNumber(time, 200, 10);

            // increment
            counter.updateRollingMax(NumerusRollingNumberEvent.THREAD_MAX_ACTIVE, 10);

            // we should have 1 bucket
            assertEquals(1, counter.buckets.size());

            // the count should be 10
            assertEquals(10, counter.buckets.getLast().getMaxUpdater(NumerusRollingNumberEvent.THREAD_MAX_ACTIVE).max());
            assertEquals(10, counter.getRollingMaxValue(NumerusRollingNumberEvent.THREAD_MAX_ACTIVE));

            // sleep to get to a new bucket
            time.increment(counter.getBucketSizeInMilliseconds() * 3);

            // increment again in latest bucket
            counter.updateRollingMax(NumerusRollingNumberEvent.THREAD_MAX_ACTIVE, 20);

            // we should have 4 buckets
            assertEquals(4, counter.buckets.size());

            // the max
            assertEquals(20, counter.buckets.getLast().getMaxUpdater(NumerusRollingNumberEvent.THREAD_MAX_ACTIVE).max());

            // counts per bucket
            long values[] = counter.getValues(NumerusRollingNumberEvent.THREAD_MAX_ACTIVE);
            assertEquals(10, values[0]); // oldest bucket
            assertEquals(0, values[1]);
            assertEquals(0, values[2]);
            assertEquals(20, values[3]); // latest bucket

        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception: " + e.getMessage());
        }
    }

    @Test
    public void testUpdateMax2() {
        MockedTime time = new MockedTime();
        try {
            NumerusRollingNumber counter = new NumerusRollingNumber(time, 200, 10);

            // increment
            counter.updateRollingMax(NumerusRollingNumberEvent.THREAD_MAX_ACTIVE, 10);
            counter.updateRollingMax(NumerusRollingNumberEvent.THREAD_MAX_ACTIVE, 30);
            counter.updateRollingMax(NumerusRollingNumberEvent.THREAD_MAX_ACTIVE, 20);

            // we should have 1 bucket
            assertEquals(1, counter.buckets.size());

            // the count should be 30
            assertEquals(30, counter.buckets.getLast().getMaxUpdater(NumerusRollingNumberEvent.THREAD_MAX_ACTIVE).max());
            assertEquals(30, counter.getRollingMaxValue(NumerusRollingNumberEvent.THREAD_MAX_ACTIVE));

            // sleep to get to a new bucket
            time.increment(counter.getBucketSizeInMilliseconds() * 3);

            counter.updateRollingMax(NumerusRollingNumberEvent.THREAD_MAX_ACTIVE, 30);
            counter.updateRollingMax(NumerusRollingNumberEvent.THREAD_MAX_ACTIVE, 30);
            counter.updateRollingMax(NumerusRollingNumberEvent.THREAD_MAX_ACTIVE, 50);

            // we should have 4 buckets
            assertEquals(4, counter.buckets.size());

            // the count
            assertEquals(50, counter.buckets.getLast().getMaxUpdater(NumerusRollingNumberEvent.THREAD_MAX_ACTIVE).max());
            assertEquals(50, counter.getValueOfLatestBucket(NumerusRollingNumberEvent.THREAD_MAX_ACTIVE));

            // values per bucket
            long values[] = counter.getValues(NumerusRollingNumberEvent.THREAD_MAX_ACTIVE);
            assertEquals(30, values[0]); // oldest bucket
            assertEquals(0, values[1]);
            assertEquals(0, values[2]);
            assertEquals(50, values[3]); // latest bucket

        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception: " + e.getMessage());
        }
    }

    @Test
    public void testMaxValue() {
        MockedTime time = new MockedTime();
        try {
            NumerusRollingNumberEvent type = NumerusRollingNumberEvent.THREAD_MAX_ACTIVE;

            NumerusRollingNumber counter = new NumerusRollingNumber(time, 200, 10);

            counter.updateRollingMax(type, 10);

            // sleep to get to a new bucket
            time.increment(counter.getBucketSizeInMilliseconds());

            counter.updateRollingMax(type, 30);

            // sleep to get to a new bucket
            time.increment(counter.getBucketSizeInMilliseconds());

            counter.updateRollingMax(type, 40);

            // sleep to get to a new bucket
            time.increment(counter.getBucketSizeInMilliseconds());

            counter.updateRollingMax(type, 15);

            assertEquals(40, counter.getRollingMaxValue(type));

        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception: " + e.getMessage());
        }
    }

    @Test
    public void testEmptySum() {
        MockedTime time = new MockedTime();
        NumerusRollingNumberEvent type = NumerusRollingNumberEvent.COLLAPSED;
        NumerusRollingNumber counter = new NumerusRollingNumber(time, 200, 10);
        assertEquals(0, counter.getRollingSum(type));
    }

    @Test
    public void testEmptyMax() {
        MockedTime time = new MockedTime();
        NumerusRollingNumberEvent type = NumerusRollingNumberEvent.THREAD_MAX_ACTIVE;
        NumerusRollingNumber counter = new NumerusRollingNumber(time, 200, 10);
        assertEquals(0, counter.getRollingMaxValue(type));
    }

    @Test
    public void testEmptyLatestValue() {
        MockedTime time = new MockedTime();
        NumerusRollingNumberEvent type = NumerusRollingNumberEvent.THREAD_MAX_ACTIVE;
        NumerusRollingNumber counter = new NumerusRollingNumber(time, 200, 10);
        assertEquals(0, counter.getValueOfLatestBucket(type));
    }

    @Test
    public void testRolling() {
        MockedTime time = new MockedTime();
        NumerusRollingNumberEvent type = NumerusRollingNumberEvent.THREAD_MAX_ACTIVE;
        NumerusRollingNumber counter = new NumerusRollingNumber(time, 20, 2);
        // iterate over 20 buckets on a queue sized for 2
        for (int i = 0; i < 20; i++) {
            // first bucket
            counter.getCurrentBucket();
            try {
                time.increment(counter.getBucketSizeInMilliseconds());
            } catch (Exception e) {
                // ignore
            }

            assertEquals(2, counter.getValues(type).length);

            counter.getValueOfLatestBucket(type);

            // System.out.println("Head: " + counter.buckets.state.get().head);
            // System.out.println("Tail: " + counter.buckets.state.get().tail);
        }
    }

    @Test
    public void testCumulativeCounterAfterRolling() {
        MockedTime time = new MockedTime();
        NumerusRollingNumberEvent type = NumerusRollingNumberEvent.SUCCESS;
        NumerusRollingNumber counter = new NumerusRollingNumber(time, 20, 2);

        assertEquals(0, counter.getCumulativeSum(type));

        // iterate over 20 buckets on a queue sized for 2
        for (int i = 0; i < 20; i++) {
            // first bucket
            counter.increment(type);
            try {
                time.increment(counter.getBucketSizeInMilliseconds());
            } catch (Exception e) {
                // ignore
            }

            assertEquals(2, counter.getValues(type).length);

            counter.getValueOfLatestBucket(type);

        }

        // cumulative count should be 20 (for the number of loops above) regardless of buckets rolling
        assertEquals(20, counter.getCumulativeSum(type));
    }

    @Test
    public void testCumulativeCounterAfterRollingAndReset() {
        MockedTime time = new MockedTime();
        NumerusRollingNumberEvent type = NumerusRollingNumberEvent.SUCCESS;
        NumerusRollingNumber counter = new NumerusRollingNumber(time, 20, 2);

        assertEquals(0, counter.getCumulativeSum(type));

        // iterate over 20 buckets on a queue sized for 2
        for (int i = 0; i < 20; i++) {
            // first bucket
            counter.increment(type);
            try {
                time.increment(counter.getBucketSizeInMilliseconds());
            } catch (Exception e) {
                // ignore
            }

            assertEquals(2, counter.getValues(type).length);

            counter.getValueOfLatestBucket(type);

            if (i == 5 || i == 15) {
                // simulate a reset occurring every once in a while
                // so we ensure the absolute sum is handling it okay
                counter.reset();
            }
        }

        // cumulative count should be 20 (for the number of loops above) regardless of buckets rolling
        assertEquals(20, counter.getCumulativeSum(type));
    }

    @Test
    public void testCumulativeCounterAfterRollingAndReset2() {
        MockedTime time = new MockedTime();
        NumerusRollingNumberEvent type = NumerusRollingNumberEvent.SUCCESS;
        NumerusRollingNumber counter = new NumerusRollingNumber(time, 20, 2);

        assertEquals(0, counter.getCumulativeSum(type));

        counter.increment(type);
        counter.increment(type);
        counter.increment(type);

        // iterate over 20 buckets on a queue sized for 2
        for (int i = 0; i < 20; i++) {
            try {
                time.increment(counter.getBucketSizeInMilliseconds());
            } catch (Exception e) {
                // ignore
            }

            if (i == 5 || i == 15) {
                // simulate a reset occurring every once in a while
                // so we ensure the absolute sum is handling it okay
                counter.reset();
            }
        }

        // no increments during the loop, just some before and after
        counter.increment(type);
        counter.increment(type);

        // cumulative count should be 5 regardless of buckets rolling
        assertEquals(5, counter.getCumulativeSum(type));
    }

    @Test
    public void testCumulativeCounterAfterRollingAndReset3() {
        MockedTime time = new MockedTime();
        NumerusRollingNumberEvent type = NumerusRollingNumberEvent.SUCCESS;
        NumerusRollingNumber counter = new NumerusRollingNumber(time, 20, 2);

        assertEquals(0, counter.getCumulativeSum(type));

        counter.increment(type);
        counter.increment(type);
        counter.increment(type);

        // iterate over 20 buckets on a queue sized for 2
        for (int i = 0; i < 20; i++) {
            try {
                time.increment(counter.getBucketSizeInMilliseconds());
            } catch (Exception e) {
                // ignore
            }
        }

        // since we are rolling over the buckets it should reset naturally

        // no increments during the loop, just some before and after
        counter.increment(type);
        counter.increment(type);

        // cumulative count should be 5 regardless of buckets rolling
        assertEquals(5, counter.getCumulativeSum(type));
    }

    private static class MockedTime implements Time {

        private AtomicInteger time = new AtomicInteger(0);

        @Override
        public long getCurrentTimeInMillis() {
            return time.get();
        }

        public void increment(int millis) {
            time.addAndGet(millis);
        }

    }

}
