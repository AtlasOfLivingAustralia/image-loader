package au.org.ala.images.upload

import groovy.json.JsonBuilder
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import org.apache.http.entity.mime.HttpMultipartMode
import org.apache.http.entity.mime.MultipartEntity
import org.apache.http.entity.mime.content.StringBody
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class JobDispatcher {

    private ExecutorService _threadPool
    private String _serviceBase
    private LinkedBlockingQueue<UploadJob> _errors;
    private long _startTimeMillis
    private AtomicInteger _count = new AtomicInteger(0);

    public JobDispatcher(int threadCount, String serviceBase) {
        _threadPool = Executors.newFixedThreadPool(threadCount);
        _serviceBase = serviceBase
        _errors = new LinkedBlockingQueue<UploadJob>()
        _startTimeMillis = new Date().getTime()
    }

    public void addJob(final UploadJob job) {
        _threadPool.submit({

            def results = postMultipart("${_serviceBase}/ws/uploadImage", job.imageUrl, job.metaData)
            if (!(results?.status == 200 && (results.content.success as Boolean))) {
                job.results = results
                _errors.add(job)
            }
            def count = _count.incrementAndGet()
            if (count % 50 == 0) {
                def now = new Date()
                def expiredMillis = now.getTime() - _startTimeMillis
                def ratePerSecond = (count / expiredMillis) * 1000
                println "${count} image urls sent (averaging ${ratePerSecond} images per second)"
            }

        })
    }

    public void waitUntilFinished() {
        _threadPool.shutdown();
        _threadPool.awaitTermination(2, TimeUnit.DAYS);
    }

    static def postMultipart(url, String imageUrl, Map metadata) {

        def result = [:]
        HTTPBuilder builder = new HTTPBuilder(url)
        builder.request(Method.POST) { request ->

            requestContentType : 'multipart/form-data'
            MultipartEntity content = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE)
            content.addPart("url", new StringBody(imageUrl))
            def json = new JsonBuilder(metadata).toString()
            content.addPart("metadata", new StringBody(json))

            request.setEntity(content)

            response.success = {resp, message ->
                result.status = resp.status
                result.content = message
            }

            response.failure = {resp ->
                result.status = resp.status
                result.error = "Error POSTing to ${url}"
            }

        }
        result
    }

}
