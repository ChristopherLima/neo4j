/**
 * Copyright (c) 2002-2014 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.cypher.internal.compiler.v2_1.pipes

import org.junit.Test
import org.scalatest.Assertions
import org.scalatest.mock.MockitoSugar

class QueryStateTest extends Assertions with MockitoSugar {
  @Test
  def should_keep_time_stamp() {
    //GIVEN
    val state = QueryStateHelper.empty

    //WHEN
    val ts1 = state.readTimeStamp()
    Thread.sleep(10)
    val ts2 = state.readTimeStamp()

    //THEN
    assert(ts1 === ts2, "Time has changed")
  }

  @Test
  def case_class_copying_should_still_see_same_time() {
    //GIVEN
    val state = QueryStateHelper.empty

    //WHEN
    val ts1 = state.readTimeStamp()
    Thread.sleep(10)
    val stateCopy = state.copy(params = Map.empty)


    //THEN
    assert(ts1 === stateCopy.readTimeStamp(), "Time has changed")
  }

  @Test
  def if_state_is_copied_and_time_seen_in_one_querystate_it_should_be_reflected_in_copies() {
    //GIVEN
    val state = QueryStateHelper.empty

    //WHEN
    val stateCopy = state.copy(params = Map.empty)
    val ts1 = state.readTimeStamp()
    Thread.sleep(10)


    //THEN
    assert(ts1 === stateCopy.readTimeStamp(), "Time has changed")
  }

  @Test
  def add_cleanup_task() {
    //GIVEN
    val state = QueryStateHelper.empty
    val cleanupTask = mock[() => Unit]

    //WHEN
    state.addCleanupTask(cleanupTask)

    //THEN
    assert(state.cleanupTasks == Seq(cleanupTask))
  }

  @Test
  def add_cleanup_task_copy() {
    //GIVEN
    val state = QueryStateHelper.empty
    val cleanupTask = mock[() => Unit]
    val newCleanupTask = mock[() => Unit]

    //WHEN
    state.addCleanupTask(cleanupTask)
    val copiedState = state.copy()
    state.addCleanupTask(newCleanupTask)

    //THEN
    assert(copiedState.cleanupTasks == Seq(cleanupTask, newCleanupTask))
  }
}
