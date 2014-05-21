package au.org.ala.images.upload.commands

class UploadCommand extends AbstractCommand {

    @Override
    String getCommandName() {
        return "upload"
    }

    @Override
    List<CommandArgument> getAcceptedArgs() {
        return [CommandArguments.databaseFile, CommandArguments.serviceBaseUrl, CommandArguments.uploadBatchSize, CommandArguments.limit]
    }

    @Override
    void executeImpl(Map args) {

        def uploadBatch = { batch ->
            def results = webService.uploadImages(batch)
            def statusBatch = []
            batch.each { srcImage ->
                def item = [sourceUrl: srcImage.sourceUrl, status: ""]
                if (results[srcImage.sourceUrl].success) {
                    item.status = "OK"
                }
                statusBatch << item
            }
            imageService.updateImageStatusBatch(statusBatch)
        }

        int batchSize = args.uploadBatchSize.toInteger()
        def batch = []
        def count = 0
        int limit = args.limit.toInteger()
        def imagesUploaded = 0

        imageService.eachImage { srcImage ->

            if (limit == 0 || count < limit) {

                if (srcImage.status != 'OK') {
                    batch << srcImage.metaData
                }

                if (batch.size() == batchSize) {
                    uploadBatch(batch)
                    batch = []
                    imagesUploaded += batchSize
                    println "${imagesUploaded} images uploaded."
                }
            }
        }

        if (batch) {
            uploadBatch(batch)
            println "${imagesUploaded + batch.size()} images uploaded."
        }

        println "Done."
    }
}
