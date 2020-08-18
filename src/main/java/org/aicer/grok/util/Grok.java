/*
 * Copyright 2014 American Institute for Computing Education and Research Inc
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
package org.aicer.grok.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.aicer.grok.dictionary.GrokDictionary;

/**
 *
 * @author <a href="mailto:israel@aicer.org">Israel Ekpo</a>
 *
 */
public final class Grok {

  private final Pattern compiledPattern;

  /**
   * Constructor
   */
  public Grok(final Pattern compiledPattern) {
     this.compiledPattern = compiledPattern;
  }

  /**
   * Extracts named groups from the raw data
   *
   * @param rawData
   * @return A map of group names mapped to their extracted values or null if there are no matches
   */
  public Map<String, String> extractNamedGroups(final CharSequence rawData) {

    Matcher matcher = compiledPattern.matcher(rawData);

    if (matcher.find()) {

      MatchResult r = matcher.toMatchResult();

      try {
        Map<String, Integer> namedGroups = getNamedGroups(compiledPattern);
        Map<String, String> namedGroupValues = new HashMap<>();
        for (String name : namedGroups.keySet()) {
          namedGroupValues.put(name, r.group(namedGroups.get(name)));
        }
        return namedGroupValues;
      } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
        return null;
      }
    }

    return null;
  }

  private static Map<String, Integer> getNamedGroups(Pattern regex)
          throws NoSuchMethodException, SecurityException,
          IllegalAccessException, IllegalArgumentException,
          InvocationTargetException {

    Method namedGroupsMethod = Pattern.class.getDeclaredMethod("namedGroups");
    namedGroupsMethod.setAccessible(true);

    Map<String, Integer> namedGroups = (Map<String, Integer>) namedGroupsMethod.invoke(regex);

    if (namedGroups == null) {
      throw new InternalError();
    }

    return Collections.unmodifiableMap(namedGroups);
  }
  
  private static final void displayResults(final Map<String, String> results) {
    if (results != null) {
      for(Map.Entry<String, String> entry : results.entrySet()) {
        System.out.println(entry.getKey() + "=" + entry.getValue());
      }
    }
  }

  public static void main(String[] args) {

    final String rawDataLine1 = "1234567 - israel.ekpo@massivelogdata.net cc55ZZ35 1789 Hello Grok";
    final String rawDataLine2 = "98AA541 - israel-ekpo@israelekpo.com mmddgg22 8800 Hello Grok";
    final String rawDataLine3 = "55BB778 - ekpo.israel@example.net secret123 4439 Valid Data Stream";

    final String expression = "%{EMAIL:username} %{USERNAME:password} %{INT:yearOfBirth}";

    final GrokDictionary dictionary = new GrokDictionary();

    // Load the built-in dictionaries
    dictionary.addBuiltInDictionaries();

    // Resolve all expressions loaded
    dictionary.bind();

    // Take a look at how many expressions have been loaded
    System.out.println("Dictionary Size: " + dictionary.getDictionarySize());

    Grok compiledPattern = dictionary.compileExpression(expression);

    displayResults(compiledPattern.extractNamedGroups(rawDataLine1));
    displayResults(compiledPattern.extractNamedGroups(rawDataLine2));
    displayResults(compiledPattern.extractNamedGroups(rawDataLine3));
  }
}
