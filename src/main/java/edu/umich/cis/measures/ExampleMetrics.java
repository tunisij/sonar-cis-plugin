/*
 * Example Plugin for SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package edu.umich.cis.measures;

import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;

import java.util.List;

import static java.util.Arrays.asList;

public class ExampleMetrics implements Metrics {

  public static final Metric<Integer> FILENAME_SIZE = new Metric.Builder("filename_size", "Filename Size", Metric.ValueType.INT)
          .setDescription("Number of characters of file names")
          .setDirection(Metric.DIRECTION_BETTER)
          .setQualitative(false)
          .setDomain(CoreMetrics.DOMAIN_GENERAL)
          .create();

  public static final Metric<Integer> FILENAME_SIZE_RATING = new Metric.Builder("filename_size_rating", "Filename Size Rating", Metric.ValueType.RATING)
          .setDescription("Rating based on size of file names")
          .setDirection(Metric.DIRECTION_BETTER)
          .setQualitative(true)
          .setDomain(CoreMetrics.DOMAIN_GENERAL)
          .create();

  public static final Metric<Integer> FILE_ID = new Metric.Builder("file_id", "File Id", Metric.ValueType.INT)
          .setDescription("Id for database")
          .setDirection(Metric.DIRECTION_NONE)
          .setQualitative(false)
          .setHidden(true)
          .setDomain(CoreMetrics.DOMAIN_COMPLEXITY)
          .create();

  public static final Metric<Integer> COMPLEXITY_DELTA = new Metric.Builder("complexity_delta", "Complexity Delta", Metric.ValueType.INT)
          .setDescription("Complexity change since last scan")
          .setDirection(Metric.DIRECTION_WORST)
          .setQualitative(false)
          .setDomain(CoreMetrics.DOMAIN_COMPLEXITY)
          .create();

  public static final Metric<Integer> CHANGE_FREQUENCY = new Metric.Builder("change_frequency", "Change Frequency", Metric.ValueType.INT)
          .setDescription("Number of changes made to each file")
          .setDirection(Metric.DIRECTION_WORST)
          .setQualitative(false)
          .setDomain(CoreMetrics.DOMAIN_GENERAL)
          .create();

  public static final Metric<Double> CHANGE_MAINTAINABILITY = new Metric.Builder("change_maintainability", "Maintainability Delta", Metric.ValueType.PERCENT)
          .setDescription("Differences between old sqale debt ratio and new sqale debt ratio of each file")
          .setDomain(CoreMetrics.DOMAIN_MAINTAINABILITY)
          .setDirection(Metric.DIRECTION_WORST)
          .setQualitative(false)
          .create();

  @Override
  public List<Metric> getMetrics() {
    return asList(FILENAME_SIZE, FILENAME_SIZE_RATING, FILE_ID, COMPLEXITY_DELTA, CHANGE_FREQUENCY, CHANGE_MAINTAINABILITY);
  }
}