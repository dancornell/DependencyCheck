package org.codesecure.dependencycheck.dependency;
/*
 * This file is part of DependencyCheck.
 *
 * DependencyCheck is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DependencyCheck is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DependencyCheck. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) 2012 Jeremy Long. All Rights Reserved.
 */

/**
 *
 * @author Jeremy Long (jeremy.long@gmail.com)
 */
public class Identifier {

    /**
     * Constructs a new Identifier with the specified data.
     * @param type the identifier type.
     * @param value the identifier value.
     * @param title the identifier title.
     * @param url the identifier url.
     */
    Identifier(String type, String value, String title, String url) {
        this.type = type;
        this.value = value;
        this.title = title;
        this.url = url;
    }
    /**
     * The value of the identifeir
     */
    protected String value;

    /**
     * Get the value of value
     *
     * @return the value of value
     */
    public String getValue() {
        return value;
    }

    /**
     * Set the value of value
     *
     * @param value new value of value
     */
    public void setValue(String value) {
        this.value = value;
    }
    /**
     * The title of the identifeir
     */
    protected String title;

    /**
     * Get the value of title
     *
     * @return the value of title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Set the value of title
     *
     * @param title new value of title
     */
    public void setTitle(String title) {
        this.title = title;
    }
    /**
     * The url for the identifeir
     */
    protected String url;

    /**
     * Get the value of url
     *
     * @return the value of url
     */
    public String getUrl() {
        return url;
    }

    /**
     * Set the value of url
     *
     * @param url new value of url
     */
    public void setUrl(String url) {
        this.url = url;
    }
    /**
     * The type of the identifeir
     */
    protected String type;

    /**
     * Get the value of type
     *
     * @return the value of type
     */
    public String getType() {
        return type;
    }

    /**
     * <p>Set the value of type.</p><p>Example would be "CPE".</p>
     *
     * @param type new value of type
     */
    public void setType(String type) {
        this.type = type;
    }
}
