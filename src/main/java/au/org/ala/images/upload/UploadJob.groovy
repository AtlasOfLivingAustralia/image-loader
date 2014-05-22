package au.org.ala.images.upload

import au.org.ala.images.upload.commands.UploadJobContext
import au.org.ala.images.upload.service.ImageService
import au.org.ala.images.upload.service.WebService


class UploadJob implements Runnable {

    List _batch
    int _batchId
    UploadJobContext _context

    public UploadJob(int batchId, List batch, UploadJobContext context) {
        _batch = batch
        _batchId = batchId
        _context = context
    }

    @Override
    void run() {

        println "Batch ${_batchId}: Uploading ${_batch*.batchSequenceNumber}"
        def results = _context.webService.uploadImages(_batch)
        def statusBatch = []
        int successCount = 0
        int errorCount = 0
        _batch.each { srcImage ->
            def item = [sourceUrl: srcImage.sourceUrl, status: ""]
            if (results[srcImage.sourceUrl].success) {
                successCount++
                item.status = "OK"
                item.imageId = results[srcImage.sourceUrl].imageId
            } else {
                errorCount++
            }
            statusBatch << item
        }
        _context.imageService.updateImageStatusBatch(statusBatch)
        _context.uploadCount.addAndGet(successCount)
        println "Batch ${_batchId}: ${successCount} images uploaded OK."
    }

}
