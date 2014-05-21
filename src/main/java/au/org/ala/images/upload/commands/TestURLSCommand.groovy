package au.org.ala.images.upload.commands

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class TestURLSCommand extends AbstractCommand {

    @Override
    String getCommandName() {
        return "testurls"
    }

    @Override
    List<CommandArgument> getAcceptedArgs() {
        return [CommandArguments.databaseFile]
    }

    @Override
    void executeImpl(Map args) {
        AtomicInteger count = new AtomicInteger(0)
        AtomicInteger errorCount = new AtomicInteger(0)

        def threadPool = Executors.newFixedThreadPool(8);

        imageService.eachImageUrl { url ->

            threadPool.submit( {

                def status = webService.getHeadStatus(url)
                if (status != 200) {
                    errorCount.incrementAndGet()
                    println "${status}: ${url}"
                }

                if (count.incrementAndGet() % 1000 == 0) {
                    println "${count.get()} urls tested. ${errorCount.get()} Errors detected"
                }
            })
        }

        println "Waiting..."
        threadPool.shutdown()
        threadPool.awaitTermination(2, TimeUnit.DAYS)

        println "${count} urls tested. ${errorCount} errors."
    }
}
