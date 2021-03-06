package fi.jasoft.plugin.tasks

import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

/*
* Copyright 2015 John Ahlroos
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
class DirectorySearchTask extends DefaultTask {

    public static final String NAME = 'vaadinAddons'

    private final String directoryUrl = 'https://vaadin.com/Directory/resource/addon/all?detailed=true'

    private final File cachedAddonResponse = project.file('build/cache/addons.json')

    private final int maxCacheAge = TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS)

    DirectorySearchTask() {
        description = "Lists addons in the Vaadin Directory"
    }

    @TaskAction
    void run() {

        if (!cachedAddonResponse.exists()) {
            project.logger.info("Fetching addon listing from vaadin.com...")
            cachedAddonResponse.parentFile.mkdirs()
            cachedAddonResponse.createNewFile()
            cachedAddonResponse.write(directoryUrl.toURL().text)

        } else if (new Date(cachedAddonResponse.lastModified()).before(new Date(System.currentTimeMillis() - maxCacheAge))) {
            project.logger.info("Fetching addon listing from vaadin.com...")
            cachedAddonResponse.write(directoryUrl.toURL().text)
        } else {
            project.logger.info("Reading addon listing from local cache...")
        }

        def args = project.getProperties()
        def search = args.get('search', null)
        def sort = args.get('sort', null)
        def verbose = Boolean.parseBoolean(args.get('verbose', 'false'))
        listAddons(search, sort, verbose)
    }

    private void listAddons(String search, String sort, boolean verbose) {
        def json = new JsonSlurper().parseText(cachedAddonResponse.text)
        def dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")

        println ' '

        json.addon.findAll {
            return (search == null || it.name.toLowerCase().contains(search) || it.summary.toLowerCase().contains(search))

        }.sort {
            switch (sort) {
                case 'name': return it.name
                case 'description': return it.summary
                case 'date': return dateFormat.parse(it.oldestRelease.toString())
                case 'rating': return Double.parseDouble(it.avgRating)
                default: return null
            }

        }.each {
            if (verbose) {
                println 'Name: ' + it.name
                println 'Description: ' + it.summary
                println 'Url: ' + it.linkUrl
                println 'Rating: ' + it.avgRating
                if (it.artifactId != null && it.groupId != null && it.version != null) {
                    println("Dependency: \"${it.groupId}:${it.artifactId}:${it.version}\"")
                }

            } else {
                print DirectorySearchTask.truncate(it.name.toString(), 29).padRight(30)
                print DirectorySearchTask.truncate(it.summary.toString(), 49).padRight(50)
                if (it.artifactId != null && it.groupId != null && it.version != null) {
                    print(" \"${it.groupId}:${it.artifactId}:${it.version}\"")
                }
            }
            println ' '
        }
    }

    private static String truncate(String str, Integer maxLength) {
        if (str == null) {
            return ''
        }
        if (str.length() > maxLength) {
            return str[0..(maxLength - 2)] + "\u2026"
        }
        return str
    }
}
