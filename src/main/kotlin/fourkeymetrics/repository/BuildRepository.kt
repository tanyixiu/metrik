package fourkeymetrics.repository

import fourkeymetrics.model.Build
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.stereotype.Component

@Component
class BuildRepository {
    @Autowired
    private lateinit var mongoTemplate: MongoTemplate

    private val collectionName = "build"

    fun getAllBuilds(pipelineId: String): List<Build> {
        return mongoTemplate.findAll(Build::class.java, collectionName)
    }

    fun getLastBuild(pipelineId: String): Build? {
        TODO()
    }
}
