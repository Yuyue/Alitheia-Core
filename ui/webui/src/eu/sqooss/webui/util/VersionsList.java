/*
 * This file is part of the Alitheia system, developed by the SQO-OSS
 * consortium as part of the IST FP6 SQO-OSS project, number 033331.
 *
 * Copyright 2008 by the SQO-OSS consortium members <info@sqo-oss.eu>
 * Copyright 2008 by Sebastian Kuegler <sebas@kde.org>
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package eu.sqooss.webui.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import eu.sqooss.webui.datatype.Version;

public class VersionsList extends ArrayList<Version> {

    /**
     * Class serial
     */
    private static final long serialVersionUID = -2171890702868485954L;

    /**
     * Returns the list of project versions sorted by their version number.
     * 
     * @return The list of project versions.
     */
    public SortedMap<Long, Version> sortByNumber() {
        SortedMap<Long, Version> result =
            new TreeMap<Long, Version>();
        for (Version nextVersion : this)
            result.put(nextVersion.getNumber(), nextVersion);
        return result;
    }

    /**
     * Returns the list of project versions sorted by their Id.
     * 
     * @return The list of project versions.
     */
    public SortedMap<Long, Version> sortById() {
        SortedMap<Long, Version> result =
            new TreeMap<Long, Version>();
        for (Version nextVersion : this)
            result.put(nextVersion.getId(), nextVersion);
        return result;
    }

    /**
     * Gets the project version with the given version number.
     * 
     * @param number the project version's number
     * 
     * @return The project version object, or <code>null</code> if a project
     *  version with the given version number can not be found in this list.
     */
    public Version getVersionByNumber(Long number) {
        if (number == null) return null;
        return sortByNumber().get(number);
    }

    /**
     * Gets the project version with the given Id.
     * 
     * @param id the project version's Id
     * 
     * @return The project version object, or <code>null</code> if a project
     *   version with the given Id can not be found in this list.
     */
    public Version getVersionById(Long id) {
        return sortById().get(id);
    }

    /**
     * Gets the list of version numbers of all project versions in this list,
     * indexed by their project version Id.
     * 
     * @return The list of version numbers, or an empty list when none are
     *   found.
     */
    public Map<Long, Long> getVersionNumber() {
        Map<Long, Long> result = new HashMap<Long, Long>();
        for (Version nextVersion : this)
            result.put(nextVersion.getId(), nextVersion.getNumber());
        return result;
    }
}