package fourkeymetrics.model

import org.apache.logging.log4j.util.Strings

enum class BuildStatus {
    SUCCESS,
    FAILED,
    ABORTED,
    FAILURE,
    IN_PROGRESS
}

class Stage(
    var name: String = Strings.EMPTY,
    var status: BuildStatus = BuildStatus.FAILED,
    var startTimeMillis: Long = 0,
    var durationMillis: Long = 0,
    var pauseDurationMillis: Long = 0
) {
    fun getStageDoneTime(): Long {
        return this.startTimeMillis + this.durationMillis + this.pauseDurationMillis
    }
}

data class Commit(
    var commitId: String = Strings.EMPTY,
    var timestamp: Long = 0,
    var date: String = Strings.EMPTY,
    var msg: String = Strings.EMPTY
)

class Build(
    var pipelineId: String = Strings.EMPTY, var number: Int = 0,
    var result: BuildStatus? = BuildStatus.FAILED, var duration: Long = 0,
    var timestamp: Long = 0, var url: String = Strings.EMPTY,
    var stages: List<Stage> = emptyList(), var changeSets: List<Commit> = emptyList()
) {

    fun containsGivenDeploymentInGivenTimeRange(
        deployStageName: String,
        stageStatus: BuildStatus,
        startTimestamp: Long,
        endTimestamp: Long
    ): Boolean {
        val stage = this.stages.find {
            it.name == deployStageName
                    && it.status == stageStatus
                    && it.getStageDoneTime() in startTimestamp..endTimestamp
        }
        return stage != null
    }

    fun containsGivenDeploymentBeforeGivenTimestamp(
        deployStageName: String,
        stageStatus: BuildStatus,
        timestamp: Long
    ): Boolean {
        val stage = this.stages.find {
            it.name == deployStageName
                    && it.status == stageStatus
                    && it.getStageDoneTime() < timestamp
        }
        return stage != null
    }


    fun findGivenStage(deployStageName: String, stageStatus: BuildStatus): Stage? {
        return this.stages.find {
            it.name == deployStageName && it.status == stageStatus
        }
    }
}


