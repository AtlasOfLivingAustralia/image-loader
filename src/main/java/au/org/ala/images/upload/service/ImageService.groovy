package au.org.ala.images.upload.service

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.sql.BatchingPreparedStatementWrapper
import groovy.sql.Sql

import java.sql.ResultSet

class ImageService {

    Sql _sql

    public ImageService(String filename) {
        print "Connecting to database '${filename}'..."
        _sql = Sql.newInstance("jdbc:hsqldb:file:${filename}", "SA", "")
        println "OK"
    }

    public void shutdown() {
        _sql.execute("SHUTDOWN")
    }

    public void dumpImageTable() {
        def count = 0;
        _sql.eachRow("select * from image") { row ->
            println row
        }
    }

    public void initializeDatabase(String[] columnHeaders) {
        _sql.execute("drop table image if exists;");
        _sql.execute("create table image (id BIGINT IDENTITY, sourceUrl varchar(255), metadata varchar(4096), status VARCHAR(25), imageIdentifier VARCHAR(50))")

        _sql.execute("CREATE INDEX url_idx ON image (sourceUrl);")
    }

    public void insertImages(List<Map> imageMaps) {
        _sql.withBatch(0, "INSERT INTO image (sourceUrl, metadata, status) values (?,?,?)") { BatchingPreparedStatementWrapper ps ->
            imageMaps.each { imageMap ->
                def metadataJson = new JsonBuilder(imageMap.metadata).toString()
                ps.addBatch(imageMap.sourceUrl, metadataJson, "")
            }
        }
        _sql.commit()
    }

    public eachImageUrl(Closure closure) {
        _sql.eachRow("select sourceUrl from image") { row ->
            if (closure) {
                closure(row[0])
            }
        }
    }

    public eachImage(Closure closure) {
        _sql.eachRow('select sourceUrl, metadata, status from image') { row ->
            if (closure) {
                def metadata = new JsonSlurper().parseText(row[1])
                def map = [sourceUrl: row[0], metaData: metadata, status: row[2] ]
                closure(map)
            }
        }
    }

    public void updateImageStatusBatch(List images) {
        if (images) {
            def rows = _sql.withBatch(0, "UPDATE image SET status = :status, imageIdentifier = :imageId WHERE sourceUrl = :sourceUrl") { BatchingPreparedStatementWrapper ps ->
                images.each { image ->
                    ps.addBatch([sourceUrl: image.sourceUrl, status: image.status, imageId: image.imageId ?: ''])
                }
            }
            _sql.commit()
        }
    }

    public int countImages() {
        return _sql.firstRow('SELECT COUNT(sourceUrl) AS imageCount FROM image;').imageCount
    }

    public int countByStatus(String status) {
        return _sql.firstRow("SELECT COUNT(sourceUrl) AS imageCount FROM image WHERE status = ?;", [status]).imageCount
    }

}
