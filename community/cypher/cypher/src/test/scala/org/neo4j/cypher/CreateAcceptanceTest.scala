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
package org.neo4j.cypher

import org.scalatest.{FunSuite, Matchers}
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.neo4j.graphdb.Relationship

@RunWith(classOf[JUnitRunner])
class CreateAcceptanceTest extends ExecutionEngineFunSuite {

  test("using an undirected relationship pattern should fail on create") {
    evaluating {
      executeScalar[Relationship]("create (a {id: 2})-[r:KNOWS]-(b {id: 1}) RETURN r")
    }  should produce[SyntaxException]
  }

}
