package au.org.ala.images.upload.service

import groovy.json.JsonBuilder
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import net.sf.json.JSON
import org.apache.http.entity.mime.HttpMultipartMode
import org.apache.http.entity.mime.MultipartEntity
import org.apache.http.entity.mime.content.StringBody

class WebService {

    private String _baseUrl

    public WebService(String serviceBaseUrl) {
        _baseUrl = serviceBaseUrl
    }

    public Map getImageInfo(List images) {

        def url = "${_baseUrl}/ws/findImagesByOriginalFilename"
        def filenameList = images*.sourceUrl

        def results = postJSON(url, [filenames: filenameList])

        if (results.status == 200) {
            def resultsMap = results.content.results
            def map = [:]
            images.each { img ->

                def imgResults = resultsMap[img.sourceUrl]
                if (imgResults && imgResults.count > 0) {
                    map[img.sourceUrl] = imgResults.images[0]
                }
            }
            return map
        } else {
            println results
        }
        return null
    }

    static def postJSON(url, Map params) {
        def result = [:]
        HTTPBuilder builder = new HTTPBuilder(url)
        builder.request(Method.POST, ContentType.JSON) { request ->

            requestContentType : 'application/JSON'
            body = new JsonBuilder(params).toString()

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

    static def postMultipart(url, Map params) {

        def result = [:]
        HTTPBuilder builder = new HTTPBuilder(url)
        builder.request(Method.POST) { request ->

            requestContentType : 'multipart/form-data'
            MultipartEntity content = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE)
            params.each { kvp ->
                content.addPart(kvp.key, new StringBody(kvp.value))
            }

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
