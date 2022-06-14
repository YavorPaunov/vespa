// Copyright Yahoo. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#include <vespa/vespalib/testkit/test_kit.h>
#include <vespa/searchcore/proton/matching/matching_stats.h>

#include <vespa/log/log.h>
LOG_SETUP("matching_stats_test");

using namespace proton::matching;

TEST("requireThatDocCountsAddUp") {
    MatchingStats stats;
    EXPECT_EQUAL(0u, stats.docidSpaceCovered());
    EXPECT_EQUAL(0u, stats.docsMatched());
    EXPECT_EQUAL(0u, stats.docsRanked());
    EXPECT_EQUAL(0u, stats.docsReRanked());
    EXPECT_EQUAL(0u, stats.queries());
    EXPECT_EQUAL(0u, stats.limited_queries());
    {
        MatchingStats rhs;
        EXPECT_EQUAL(&rhs.docidSpaceCovered(10000), &rhs);
        EXPECT_EQUAL(&rhs.docsMatched(1000), &rhs);
        EXPECT_EQUAL(&rhs.docsRanked(100), &rhs);
        EXPECT_EQUAL(&rhs.docsReRanked(10), &rhs);
        EXPECT_EQUAL(&rhs.queries(2), &rhs);
        EXPECT_EQUAL(&rhs.limited_queries(1), &rhs);
        EXPECT_EQUAL(&stats.add(rhs), &stats);
    }
    EXPECT_EQUAL(10000u, stats.docidSpaceCovered());
    EXPECT_EQUAL(1000u, stats.docsMatched());
    EXPECT_EQUAL(100u, stats.docsRanked());
    EXPECT_EQUAL(10u, stats.docsReRanked());
    EXPECT_EQUAL(2u, stats.queries());
    EXPECT_EQUAL(1u, stats.limited_queries());
    EXPECT_EQUAL(&stats.add(MatchingStats().docidSpaceCovered(10000).docsMatched(1000).docsRanked(100)
                            .docsReRanked(10).queries(2).limited_queries(1)), &stats);
    EXPECT_EQUAL(20000u, stats.docidSpaceCovered());
    EXPECT_EQUAL(2000u, stats.docsMatched());
    EXPECT_EQUAL(200u, stats.docsRanked());
    EXPECT_EQUAL(20u, stats.docsReRanked());
    EXPECT_EQUAL(4u, stats.queries());
    EXPECT_EQUAL(2u, stats.limited_queries());
}

TEST("requireThatAverageTimesAreRecorded") {
    MatchingStats stats;
    EXPECT_APPROX(0.0, stats.matchTimeAvg(), 0.00001);
    EXPECT_APPROX(0.0, stats.groupingTimeAvg(), 0.00001);
    EXPECT_APPROX(0.0, stats.rerankTimeAvg(), 0.00001);
    EXPECT_APPROX(0.0, stats.querySetupTimeAvg(), 0.00001);
    EXPECT_APPROX(0.0, stats.queryLatencyAvg(), 0.00001);
    EXPECT_EQUAL(0u, stats.matchTimeCount());
    EXPECT_EQUAL(0u, stats.groupingTimeCount());
    EXPECT_EQUAL(0u, stats.rerankTimeCount());
    EXPECT_EQUAL(0u, stats.querySetupTimeCount());
    EXPECT_EQUAL(0u, stats.queryLatencyCount());
    stats.matchTime(0.01).groupingTime(0.1).rerankTime(0.5).querySetupTime(2.0).queryLatency(1.0);
    EXPECT_APPROX(0.01, stats.matchTimeAvg(), 0.00001);
    EXPECT_APPROX(0.1, stats.groupingTimeAvg(), 0.00001);
    EXPECT_APPROX(0.5, stats.rerankTimeAvg(), 0.00001);
    EXPECT_APPROX(2.0, stats.querySetupTimeAvg(), 0.00001);
    EXPECT_APPROX(1.0, stats.queryLatencyAvg(), 0.00001);
    stats.add(MatchingStats().matchTime(0.03).groupingTime(0.3).rerankTime(1.5).querySetupTime(6.0).queryLatency(3.0));
    EXPECT_APPROX(0.02, stats.matchTimeAvg(), 0.00001);
    EXPECT_APPROX(0.2, stats.groupingTimeAvg(), 0.00001);
    EXPECT_APPROX(1.0, stats.rerankTimeAvg(), 0.00001);
    EXPECT_APPROX(4.0, stats.querySetupTimeAvg(), 0.00001);
    EXPECT_APPROX(2.0, stats.queryLatencyAvg(), 0.00001);
    stats.add(MatchingStats().matchTime(0.05)
              .groupingTime(0.5)
              .rerankTime(2.5)
              .querySetupTime(10.0)
              .queryLatency(5.0));
    stats.add(MatchingStats().matchTime(0.05).matchTime(0.03)
              .groupingTime(0.5).groupingTime(0.3)
              .rerankTime(2.5).rerankTime(1.5)
              .querySetupTime(10.0).querySetupTime(6.0)
              .queryLatency(5.0).queryLatency(3.0));
    EXPECT_APPROX(0.03, stats.matchTimeAvg(), 0.00001);
    EXPECT_APPROX(0.3, stats.groupingTimeAvg(), 0.00001);
    EXPECT_APPROX(1.5, stats.rerankTimeAvg(), 0.00001);
    EXPECT_APPROX(6.0, stats.querySetupTimeAvg(), 0.00001);
    EXPECT_APPROX(3.0, stats.queryLatencyAvg(), 0.00001);
    EXPECT_EQUAL(4u, stats.matchTimeCount());
    EXPECT_EQUAL(4u, stats.groupingTimeCount());
    EXPECT_EQUAL(4u, stats.rerankTimeCount());
    EXPECT_EQUAL(4u, stats.querySetupTimeCount());
    EXPECT_EQUAL(4u, stats.queryLatencyCount());
}

TEST("requireThatMinMaxTimesAreRecorded") {
    MatchingStats stats;
    EXPECT_APPROX(0.0, stats.matchTimeMin(), 0.00001);
    EXPECT_APPROX(0.0, stats.groupingTimeMin(), 0.00001);
    EXPECT_APPROX(0.0, stats.rerankTimeMin(), 0.00001);
    EXPECT_APPROX(0.0, stats.querySetupTimeMin(), 0.00001);
    EXPECT_APPROX(0.0, stats.queryLatencyMin(), 0.00001);
    EXPECT_APPROX(0.0, stats.matchTimeMax(), 0.00001);
    EXPECT_APPROX(0.0, stats.groupingTimeMax(), 0.00001);
    EXPECT_APPROX(0.0, stats.rerankTimeMax(), 0.00001);
    EXPECT_APPROX(0.0, stats.querySetupTimeMax(), 0.00001);
    EXPECT_APPROX(0.0, stats.queryLatencyMax(), 0.00001);
    stats.matchTime(0.01).groupingTime(0.1).rerankTime(0.5).querySetupTime(2.0).queryLatency(1.0);
    EXPECT_APPROX(0.01, stats.matchTimeMin(), 0.00001);
    EXPECT_APPROX(0.1, stats.groupingTimeMin(), 0.00001);
    EXPECT_APPROX(0.5, stats.rerankTimeMin(), 0.00001);
    EXPECT_APPROX(2.0, stats.querySetupTimeMin(), 0.00001);
    EXPECT_APPROX(1.0, stats.queryLatencyMin(), 0.00001);
    EXPECT_APPROX(0.01, stats.matchTimeMax(), 0.00001);
    EXPECT_APPROX(0.1, stats.groupingTimeMax(), 0.00001);
    EXPECT_APPROX(0.5, stats.rerankTimeMax(), 0.00001);
    EXPECT_APPROX(2.0, stats.querySetupTimeMax(), 0.00001);
    EXPECT_APPROX(1.0, stats.queryLatencyMax(), 0.00001);
    stats.add(MatchingStats().matchTime(0.03).groupingTime(0.3).rerankTime(1.5).querySetupTime(6.0).queryLatency(3.0));
    EXPECT_APPROX(0.01, stats.matchTimeMin(), 0.00001);
    EXPECT_APPROX(0.1, stats.groupingTimeMin(), 0.00001);
    EXPECT_APPROX(0.5, stats.rerankTimeMin(), 0.00001);
    EXPECT_APPROX(2.0, stats.querySetupTimeMin(), 0.00001);
    EXPECT_APPROX(1.0, stats.queryLatencyMin(), 0.00001);
    EXPECT_APPROX(0.03, stats.matchTimeMax(), 0.00001);
    EXPECT_APPROX(0.3, stats.groupingTimeMax(), 0.00001);
    EXPECT_APPROX(1.5, stats.rerankTimeMax(), 0.00001);
    EXPECT_APPROX(6.0, stats.querySetupTimeMax(), 0.00001);
    EXPECT_APPROX(3.0, stats.queryLatencyMax(), 0.00001);
    stats.add(MatchingStats().matchTime(0.05)
              .groupingTime(0.5)
              .rerankTime(2.5)
              .querySetupTime(10.0)
              .queryLatency(5.0));
    stats.add(MatchingStats().matchTime(0.05).matchTime(0.03)
              .groupingTime(0.5).groupingTime(0.3)
              .rerankTime(2.5).rerankTime(1.5)
              .querySetupTime(10.0).querySetupTime(6.0)
              .queryLatency(5.0).queryLatency(3.0));
    EXPECT_APPROX(0.01, stats.matchTimeMin(), 0.00001);
    EXPECT_APPROX(0.1, stats.groupingTimeMin(), 0.00001);
    EXPECT_APPROX(0.5, stats.rerankTimeMin(), 0.00001);
    EXPECT_APPROX(2.0, stats.querySetupTimeMin(), 0.00001);
    EXPECT_APPROX(1.0, stats.queryLatencyMin(), 0.00001);
    EXPECT_APPROX(0.05, stats.matchTimeMax(), 0.00001);
    EXPECT_APPROX(0.5, stats.groupingTimeMax(), 0.00001);
    EXPECT_APPROX(2.5, stats.rerankTimeMax(), 0.00001);
    EXPECT_APPROX(10.0, stats.querySetupTimeMax(), 0.00001);
    EXPECT_APPROX(5.0, stats.queryLatencyMax(), 0.00001);
}

TEST("requireThatPartitionsAreAddedCorrectly") {
    MatchingStats all1;
    EXPECT_EQUAL(0u, all1.docidSpaceCovered());
    EXPECT_EQUAL(0u, all1.docsMatched());
    EXPECT_EQUAL(0u, all1.getNumPartitions());
    EXPECT_EQUAL(0u, all1.softDoomed());
    EXPECT_EQUAL(vespalib::duration::zero(), all1.doomOvertime());

    MatchingStats::Partition subPart;
    subPart.docsCovered(7).docsMatched(3).docsRanked(2).docsReRanked(1)
        .active_time(1.0).wait_time(0.5);
    EXPECT_EQUAL(0u, subPart.softDoomed());
    EXPECT_EQUAL(0u, subPart.softDoomed(false).softDoomed());
    EXPECT_EQUAL(1u, subPart.softDoomed(true).softDoomed());
    EXPECT_EQUAL(vespalib::duration::zero(), subPart.doomOvertime());
    EXPECT_EQUAL(1000ns, subPart.doomOvertime(1000ns).doomOvertime());
    EXPECT_EQUAL(7u, subPart.docsCovered());
    EXPECT_EQUAL(3u, subPart.docsMatched());
    EXPECT_EQUAL(2u, subPart.docsRanked());
    EXPECT_EQUAL(1u, subPart.docsReRanked());
    EXPECT_EQUAL(1.0, subPart.active_time_avg());
    EXPECT_EQUAL(0.5, subPart.wait_time_avg());
    EXPECT_EQUAL(1u, subPart.active_time_count());
    EXPECT_EQUAL(1u, subPart.wait_time_count());
    EXPECT_EQUAL(1.0, subPart.active_time_min());
    EXPECT_EQUAL(0.5, subPart.wait_time_min());
    EXPECT_EQUAL(1.0, subPart.active_time_max());
    EXPECT_EQUAL(0.5, subPart.wait_time_max());

    all1.merge_partition(subPart, 0);
    EXPECT_EQUAL(7u, all1.docidSpaceCovered());
    EXPECT_EQUAL(3u, all1.docsMatched());
    EXPECT_EQUAL(2u, all1.docsRanked());
    EXPECT_EQUAL(1u, all1.docsReRanked());
    EXPECT_EQUAL(1u, all1.getNumPartitions());
    EXPECT_EQUAL(1u, all1.softDoomed());
    EXPECT_EQUAL(1000ns, all1.doomOvertime());
    EXPECT_EQUAL(7u, all1.getPartition(0).docsCovered());
    EXPECT_EQUAL(3u, all1.getPartition(0).docsMatched());
    EXPECT_EQUAL(2u, all1.getPartition(0).docsRanked());
    EXPECT_EQUAL(1u, all1.getPartition(0).docsReRanked());
    EXPECT_EQUAL(1.0, all1.getPartition(0).active_time_avg());
    EXPECT_EQUAL(0.5, all1.getPartition(0).wait_time_avg());
    EXPECT_EQUAL(1u, all1.getPartition(0).active_time_count());
    EXPECT_EQUAL(1u, all1.getPartition(0).wait_time_count());
    EXPECT_EQUAL(1.0, all1.getPartition(0).active_time_min());
    EXPECT_EQUAL(0.5, all1.getPartition(0).wait_time_min());
    EXPECT_EQUAL(1.0, all1.getPartition(0).active_time_max());
    EXPECT_EQUAL(0.5, all1.getPartition(0).wait_time_max());
    EXPECT_EQUAL(1u, all1.getPartition(0).softDoomed());
    EXPECT_EQUAL(1000ns, all1.getPartition(0).doomOvertime());

    MatchingStats::Partition otherSubPart;
    otherSubPart.docsCovered(7).docsMatched(3).docsRanked(2).docsReRanked(1)
            .active_time(0.5).wait_time(1.0).softDoomed(true).doomOvertime(300ns);
    all1.merge_partition(otherSubPart, 1);
    EXPECT_EQUAL(1u, all1.softDoomed());
    EXPECT_EQUAL(1000ns, all1.doomOvertime());
    EXPECT_EQUAL(14u, all1.docidSpaceCovered());
    EXPECT_EQUAL(6u, all1.docsMatched());
    EXPECT_EQUAL(4u, all1.docsRanked());
    EXPECT_EQUAL(2u, all1.docsReRanked());
    EXPECT_EQUAL(2u, all1.getNumPartitions());
    EXPECT_EQUAL(3u, all1.getPartition(1).docsMatched());
    EXPECT_EQUAL(2u, all1.getPartition(1).docsRanked());
    EXPECT_EQUAL(1u, all1.getPartition(1).docsReRanked());
    EXPECT_EQUAL(0.5, all1.getPartition(1).active_time_avg());
    EXPECT_EQUAL(1.0, all1.getPartition(1).wait_time_avg());
    EXPECT_EQUAL(1u, all1.getPartition(1).active_time_count());
    EXPECT_EQUAL(1u, all1.getPartition(1).wait_time_count());
    EXPECT_EQUAL(0.5, all1.getPartition(1).active_time_min());
    EXPECT_EQUAL(1.0, all1.getPartition(1).wait_time_min());
    EXPECT_EQUAL(0.5, all1.getPartition(1).active_time_max());
    EXPECT_EQUAL(1.0, all1.getPartition(1).wait_time_max());
    EXPECT_EQUAL(1u, all1.getPartition(1).softDoomed());
    EXPECT_EQUAL(300ns, all1.getPartition(1).doomOvertime());

    MatchingStats all2;
    all2.merge_partition(otherSubPart, 0);
    all2.merge_partition(subPart, 1);

    all1.add(all2);
    EXPECT_EQUAL(2u, all1.softDoomed());
    EXPECT_EQUAL(1000ns, all1.doomOvertime());
    EXPECT_EQUAL(28u, all1.docidSpaceCovered());
    EXPECT_EQUAL(12u, all1.docsMatched());
    EXPECT_EQUAL(8u, all1.docsRanked());
    EXPECT_EQUAL(4u, all1.docsReRanked());
    EXPECT_EQUAL(2u, all1.getNumPartitions());
    EXPECT_EQUAL(6u, all1.getPartition(0).docsMatched());
    EXPECT_EQUAL(4u, all1.getPartition(0).docsRanked());
    EXPECT_EQUAL(2u, all1.getPartition(0).docsReRanked());
    EXPECT_EQUAL(0.75, all1.getPartition(0).active_time_avg());
    EXPECT_EQUAL(0.75, all1.getPartition(0).wait_time_avg());
    EXPECT_EQUAL(2u, all1.getPartition(0).active_time_count());
    EXPECT_EQUAL(2u, all1.getPartition(0).wait_time_count());
    EXPECT_EQUAL(0.5, all1.getPartition(0).active_time_min());
    EXPECT_EQUAL(0.5, all1.getPartition(0).wait_time_min());
    EXPECT_EQUAL(1.0, all1.getPartition(0).active_time_max());
    EXPECT_EQUAL(1.0, all1.getPartition(0).wait_time_max());
    EXPECT_EQUAL(2u, all1.getPartition(0).softDoomed());
    EXPECT_EQUAL(1000ns, all1.getPartition(0).doomOvertime());
    EXPECT_EQUAL(6u, all1.getPartition(1).docsMatched());
    EXPECT_EQUAL(4u, all1.getPartition(1).docsRanked());
    EXPECT_EQUAL(2u, all1.getPartition(1).docsReRanked());
    EXPECT_EQUAL(0.75, all1.getPartition(1).active_time_avg());
    EXPECT_EQUAL(0.75, all1.getPartition(1).wait_time_avg());
    EXPECT_EQUAL(2u, all1.getPartition(1).active_time_count());
    EXPECT_EQUAL(2u, all1.getPartition(1).wait_time_count());
    EXPECT_EQUAL(0.5, all1.getPartition(1).active_time_min());
    EXPECT_EQUAL(0.5, all1.getPartition(1).wait_time_min());
    EXPECT_EQUAL(1.0, all1.getPartition(1).active_time_max());
    EXPECT_EQUAL(1.0, all1.getPartition(1).wait_time_max());
    EXPECT_EQUAL(2u, all1.getPartition(1).softDoomed());
    EXPECT_EQUAL(1000ns, all1.getPartition(1).doomOvertime());
}

TEST("requireThatSoftDoomIsSetAndAdded") {
    MatchingStats stats;
    MatchingStats stats2;
    EXPECT_EQUAL(0ul, stats.softDoomed());
    EXPECT_EQUAL(0.5, stats.softDoomFactor());
    stats.softDoomFactor(0.7);
    stats.softDoomed(3);
    EXPECT_EQUAL(3ul, stats.softDoomed());
    EXPECT_EQUAL(0.7, stats.softDoomFactor());
    stats2.add(stats);
    EXPECT_EQUAL(3ul, stats2.softDoomed());
    EXPECT_EQUAL(0.5, stats2.softDoomFactor());  // Not affected by add
}

TEST("requireThatSoftDoomFacorIsComputedCorrectlyForDownAdjustment") {
    MatchingStats stats;
    EXPECT_EQUAL(0ul, stats.softDoomed());
    EXPECT_EQUAL(0.5, stats.softDoomFactor());
    stats.softDoomed(1);
    stats.updatesoftDoomFactor(1000ms, 500ms, 2000ms);
    EXPECT_EQUAL(1ul, stats.softDoomed());
    EXPECT_EQUAL(0.47, stats.softDoomFactor());
    stats.updatesoftDoomFactor(1000ms, 500ms, 2000ms);
    EXPECT_EQUAL(1ul, stats.softDoomed());
    EXPECT_EQUAL(0.44, stats.softDoomFactor());
    stats.updatesoftDoomFactor(900us, 500ms, 2000ms);   // hard limits less than 1ms should be ignored
    EXPECT_EQUAL(1ul, stats.softDoomed());
    EXPECT_EQUAL(0.44, stats.softDoomFactor());
    stats.updatesoftDoomFactor(1000ms, 900us, 2000ms);   // soft limits less than 1ms should be ignored
    EXPECT_EQUAL(1ul, stats.softDoomed());
    EXPECT_EQUAL(0.44, stats.softDoomFactor());
    stats.updatesoftDoomFactor(1000ms, 500ms, 10s);      // Prevent changes above 10%
    EXPECT_EQUAL(1ul, stats.softDoomed());
    EXPECT_EQUAL(0.396, stats.softDoomFactor());
}

TEST("requireThatSoftDoomFacorIsComputedCorrectlyForUpAdjustment") {
    MatchingStats stats;
    EXPECT_EQUAL(0ul, stats.softDoomed());
    EXPECT_EQUAL(0.5, stats.softDoomFactor());
    stats.softDoomed(1);
    stats.updatesoftDoomFactor(1s, 900ms, 100ms);
    EXPECT_EQUAL(1ul, stats.softDoomed());
    EXPECT_EQUAL(0.508, stats.softDoomFactor());
    stats.updatesoftDoomFactor(1s, 900ms, 100ms);
    EXPECT_EQUAL(1ul, stats.softDoomed());
    EXPECT_EQUAL(0.516, stats.softDoomFactor());
    stats.updatesoftDoomFactor(900us, 900ms, 100ms);   // hard limits less than 1ms should be ignored
    EXPECT_EQUAL(1ul, stats.softDoomed());
    EXPECT_EQUAL(0.516, stats.softDoomFactor());
    stats.updatesoftDoomFactor(1s, 900us, 100ms);   // soft limits less than 1ms should be ignored
    EXPECT_EQUAL(1ul, stats.softDoomed());
    EXPECT_EQUAL(0.516, stats.softDoomFactor());
    stats.softDoomFactor(0.1);
    stats.updatesoftDoomFactor(1s, 900ms, 1ms);      // Prevent changes above 5%
    EXPECT_EQUAL(1ul, stats.softDoomed());
    EXPECT_EQUAL(0.105, stats.softDoomFactor());
}

TEST("requireThatFactor is capped at minimum 1%") {
    MatchingStats stats;
    stats.softDoomFactor(0.01001);
    EXPECT_EQUAL(0.01001, stats.softDoomFactor());
    stats.updatesoftDoomFactor(1s, 500ms, 900ms);
    EXPECT_EQUAL(0.01, stats.softDoomFactor());
    stats.updatesoftDoomFactor(1s, 900ms, 1ms);
    EXPECT_EQUAL(0.0105, stats.softDoomFactor());
}

TEST_MAIN() {
    TEST_RUN_ALL();
}
