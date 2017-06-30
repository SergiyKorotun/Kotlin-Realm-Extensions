package com.vicpin.krealmextensions

import android.support.test.runner.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.vicpin.krealmextensions.model.TestEntity
import com.vicpin.krealmextensions.model.TestEntityPK
import com.vicpin.krealmextensions.util.TestRealmConfigurationFactory
import io.reactivex.disposables.Disposable
import io.realm.Realm
import io.realm.Sort
import io.realm.exceptions.RealmPrimaryKeyConstraintException
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import rx.Subscription
import java.util.concurrent.CountDownLatch

/**
 * Created by victor on 10/1/17.
 */
@RunWith(AndroidJUnit4::class)
class KRealmExtensionsTests {

    @get:Rule var configFactory = TestRealmConfigurationFactory()
    lateinit var realm: Realm
    lateinit var latch: CountDownLatch
    var latchReleased = false
    var disposable: Disposable? = null
    var subscription: Subscription? = null


    @Before fun setUp() {
        val realmConfig = configFactory.createConfiguration()
        realm = Realm.getInstance(realmConfig)
        latch = CountDownLatch(1)
    }

    @After fun tearDown() {
        TestEntity().deleteAll()
        TestEntityPK().deleteAll()
        realm.close()
        latchReleased = false
        disposable = null
        subscription = null
    }

    /**
     * PERSISTENCE TESTS
     */
    @Test fun testPersistEntityWithCreate() {
        TestEntity().create() //No exception expected
    }

    @Test fun testPersistEntityWithCreateManaged() {
        val result = TestEntity().createManaged(realm) //No exception expected
        assertThat(result.isManaged).isTrue()
    }

    @Test fun testPersistPKEntityWithCreate() {
        TestEntityPK(1).create() //No exception expected
    }

    @Test fun testPersistPKEntityWithCreateManaged() {
        val result = TestEntityPK(1).createManaged(realm) //No exception expected
        assertThat(result.isManaged).isTrue()
    }

    @Test(expected = IllegalArgumentException::class)
    fun testPersistEntityWithCreateOrUpdateMethod() {
        TestEntity().createOrUpdate() //Exception expected due to TestEntity has no primary key
    }

    @Test(expected = IllegalArgumentException::class)
    fun testPersistEntityWithCreateOrUpdateMethodManaged() {
        TestEntity().createOrUpdateManaged(realm) //Exception expected due to TestEntity has no primary key
    }

    fun testPersistPKEntityWithCreateOrUpdateMethod() {
        TestEntityPK(1).createOrUpdate() //No exception expected
    }

    fun testPersistPKEntityWithCreateOrUpdateMethodManaged() {
        val result = TestEntityPK(1).createOrUpdateManaged(realm) //No exception expected
        assertThat(result.isManaged).isTrue()
    }

    @Test fun testPersistEntityWithSaveMethod() {
        TestEntity().save() //No exception expected
    }

    @Test fun testPersistEntityWithSaveMethodManaged() {
        val result = TestEntity().saveManaged(realm) //No exception expected
        assertThat(result.isManaged)
    }

    @Test fun testPersistPKEntityWithSaveMethod() {
        TestEntityPK(1).save() //No exception expected
    }

    @Test fun testPersistPKEntityWithSaveMethodManaged() {
        val result = TestEntityPK(1).saveManaged(realm) //No exception expected
        assertThat(result.isManaged).isTrue()
    }

    @Test(expected = RealmPrimaryKeyConstraintException::class)
    fun testPersistPKEntityWithCreateMethodAndSamePrimaryKey() {
        TestEntityPK(1).create() //No exception expected
        TestEntityPK(1).create() //Exception expected
    }

    @Test(expected = RealmPrimaryKeyConstraintException::class)
    fun testPersistPKEntityWithCreateMethodAndSamePrimaryKeyManaged() {
        val result = TestEntityPK(1).createManaged(realm) //No exception expected
        assertThat(result.isManaged).isTrue()
        TestEntityPK(1).createManaged(realm) //Exception expected
    }

    @Test fun testPersistPKEntityListWithSaveMethod() {
        val list = listOf(TestEntityPK(1), TestEntityPK(2), TestEntityPK(3))
        list.saveAll()
    }

    @Test fun testPersistPKEntityListWithSaveMethodManaged() {
        val list = listOf(TestEntityPK(1), TestEntityPK(2), TestEntityPK(3))
        val results = list.saveAllManaged(realm)
        results.forEach { assertThat(it.isManaged).isTrue() }
    }

    @Test fun testCountPKEntity() {
        val list = listOf(TestEntityPK(1), TestEntityPK(2), TestEntityPK(3))
        list.saveAll()
        assertThat(TestEntityPK().count()).isEqualTo(3)
    }

    @Test fun testCountDuplicatePKEntity() {
        val list = listOf(TestEntityPK(1), TestEntityPK(1), TestEntityPK(1))
        list.saveAll()
        assertThat(TestEntityPK().count()).isEqualTo(1)
    }

    @Test fun testCountEntity() {
        val list = listOf(TestEntity(), TestEntity(), TestEntity())
        list.saveAll()
        assertThat(TestEntity().count()).isEqualTo(3)
    }

    /**
     * QUERY TESTS WITH EMPTY DB
     */
    @Test fun testQueryFirstObjectWithEmptyDBShouldReturnNull() {
        assertThat(TestEntity().queryFirst()).isNull()
    }

    @Test fun testAsyncQueryFirstObjectWithEmptyDBShouldReturnNull() {
        block {
            TestEntity().queryFirstAsync { assertThat(it).isNull();release() }
        }
    }

    @Test fun testQueryLastObjectWithEmptyDBShouldReturnNull() {
        assertThat(TestEntity().queryLast()).isNull()
    }

    @Test fun testQueryLastObjectWithConditionAndEmptyDBShouldReturnNull() {
        assertThat(TestEntity().queryLast { it.equalTo("name", "test") }).isNull()
    }

    @Test fun testAsyncQueryLastObjectWithEmptyDBShouldReturnNull() {
        block {
            TestEntity().queryLastAsync { assertThat(it).isNull(); release() }
        }
    }

    @Test fun testAllItemsShouldReturnEmptyCollectionWhenDBIsEmpty() {
        assertThat(TestEntity().queryAll()).hasSize(0)
    }

    @Test fun testAllItemsAsyncShouldReturnEmptyCollectionWhenDBIsEmpty() {
        block {
            TestEntity().queryAllAsync { assertThat(it).hasSize(0); release() }
        }
    }

    @Test fun testQueryConditionalWhenDBIsEmpty() {
        val result = TestEntity().query { it.equalTo("name", "test") }
        assertThat(result).hasSize(0)
    }

    @Test fun testQueryFirstItemWhenDBIsEmpty() {
        val result = TestEntity().queryFirst { it.equalTo("name", "test") }
        assertThat(result).isNull()
    }

    @Test fun testQuerySortedWhenDBIsEmpty() {
        val result = TestEntity().querySorted("name", Sort.ASCENDING) { it.equalTo("name", "test") }
        assertThat(result).hasSize(0)
    }

    /**
     * QUERY TESTS WITH POPULATED DB
     */
    @Test fun testQueryFirstItemShouldReturnFirstItemWhenDBIsNotEmpty() {
        populateDBWithTestEntityPK(numItems = 5)
        assertThat(TestEntityPK().queryFirst()).isNotNull()
        assertThat(TestEntityPK().queryFirst()?.id).isEqualTo(0)
    }

    @Test fun testAsyncQueryFirstItemShouldReturnFirstItemWhenDBIsNotEmpty() {
        populateDBWithTestEntityPK(numItems = 5)

        block {
            TestEntityPK().queryFirstAsync {
                assertThat(it).isNotNull()
                assertThat(it?.id).isEqualTo(0)
                release()
            }
        }
    }

    @Test fun testQueryLastItemShouldReturnLastItemWhenDBIsNotEmpty() {
        populateDBWithTestEntityPK(numItems = 5)
        assertThat(TestEntityPK().queryLast()?.id).isEqualTo(4)
    }

    @Test fun testQueryLastItemWithConditionShouldReturnLastItemWhenDBIsNotEmpty() {
        populateDBWithTestEntityPK(numItems = 5)
        assertThat(TestEntityPK().queryLast { it.equalTo("id", 3) }?.id).isEqualTo(3)
    }

    @Test fun testAsyncQueryLastItemShouldReturnLastItemWhenDBIsNotEmpty() {
        populateDBWithTestEntityPK(numItems = 5)

        block {
            TestEntityPK().queryLastAsync {
                assertThat(it).isNotNull()
                release()
            }
        }
    }

    @Test fun testQueryAllItemsShouldReturnAllItemsWhenDBIsNotEmpty() {
        populateDBWithTestEntity(numItems = 5)
        assertThat(TestEntity().queryAll()).hasSize(5)
    }

    @Test fun testAsyncQueryAllItemsShouldReturnAllItemsWhenDBIsNotEmpty() {
        populateDBWithTestEntity(numItems = 5)

        block {
            TestEntity().queryAllAsync { assertThat(it).hasSize(5); release() }
        }
    }


    @Test fun testQueryAllItemsAfterSaveCollection() {
        val list = listOf(TestEntityPK(1), TestEntityPK(2), TestEntityPK(3))
        list.saveAll()

        assertThat(TestEntityPK().queryAll()).hasSize(3)
    }

    /**
     * QUERY TESTS WITH WHERE STATEMENT
     */
    @Test fun testWhereQueryShouldReturnExpectedItems() {
        populateDBWithTestEntityPK(numItems = 5)
        val results = TestEntityPK().query { query -> query.equalTo("id", 1) }

        assertThat(results).hasSize(1)
        assertThat(results.first().id).isEqualTo(1)
    }

    @Test fun testAsyncWhereQueryShouldReturnExpectedItems() {
        populateDBWithTestEntityPK(numItems = 5)

        block {
            TestEntityPK().queryAsync({ query -> query.equalTo("id", 1) }) { results ->
                assertThat(results).hasSize(1)
                assertThat(results.first().id).isEqualTo(1)
                release()
            }
        }
    }

    @Test fun testWhereQueryShouldNotReturnAnyItem() {
        populateDBWithTestEntityPK(numItems = 5)
        val results = TestEntityPK().query { query -> query.equalTo("id", 6) }

        assertThat(results).hasSize(0)
    }

    @Test fun testAsyncWhereQueryShouldNotReturnAnyItem() {
        populateDBWithTestEntityPK(numItems = 5)

        block {
            TestEntityPK().queryAsync({ query -> query.equalTo("id", 6) }) { results ->
                assertThat(results).hasSize(0)
                release()
            }
        }
    }

    @Test fun testFirstItemWhenDbIsNotEmpty() {
        populateDBWithTestEntityPK(numItems = 5)

        val result = TestEntityPK().queryFirst { it.equalTo("id", 2) }

        assertThat(result).isNotNull()
        assertThat(result?.id).isEqualTo(2)
    }

    @Test fun testQueryAscendingShouldReturnOrderedResults() {
        populateDBWithTestEntityPK(numItems = 5)

        val result = TestEntityPK().querySorted("id", Sort.ASCENDING)

        assertThat(result).hasSize(5)
        assertThat(result.first().id).isEqualTo(0)
        assertThat(result.last().id).isEqualTo(4)
    }

    @Test fun testQueryDescendingShouldReturnOrderedResults() {
        populateDBWithTestEntityPK(numItems = 5)

        val result = TestEntityPK().querySorted("id", Sort.DESCENDING)

        assertThat(result).hasSize(5)
        assertThat(result.first().id).isEqualTo(4)
        assertThat(result.last().id).isEqualTo(0)
    }

    @Test fun testQueryDescendingWithFilterShouldReturnOrderedResults() {
        populateDBWithTestEntityPK(numItems = 5)

        val result = TestEntityPK().querySorted("id", Sort.DESCENDING) {
            it.lessThan("id", 3).greaterThan("id", 0)
        }

        assertThat(result).hasSize(2)
        assertThat(result.first().id).isEqualTo(2)
        assertThat(result.last().id).isEqualTo(1)
    }

    /**
     * DELETION TESTS
     */
    @Test fun testDeleteEntities() {
        populateDBWithTestEntity(numItems = 5)

        TestEntity().deleteAll()

        assertThat(TestEntity().queryAll()).hasSize(0)
    }

    @Test fun testDeleteEntitiesWithPK() {
        populateDBWithTestEntityPK(numItems = 5)

        TestEntityPK().deleteAll()

        assertThat(TestEntityPK().queryAll()).hasSize(0)
    }

    @Test fun testDeleteEntitiesWithStatement() {
        populateDBWithTestEntityPK(numItems = 5)

        TestEntityPK().delete { query -> query.equalTo("id", 1) }

        assertThat(TestEntityPK().queryAll()).hasSize(4)
    }

    /**
     * OBSERVABLE TESTS
     */
    @Test fun testQueryAllAsObservable() {

        var itemsCount = 5

        populateDBWithTestEntity(numItems = itemsCount)

        block {
            subscription = TestEntity().queryAllAsObservable().subscribe({
                assertThat(it).hasSize(itemsCount)
                release()
            })

        }

        block {
            //Add one item more to db
            ++itemsCount
            populateDBWithTestEntity(numItems = 1)
        }

        subscription?.unsubscribe()

    }

    @Test fun testQueryAsObservable() {

        populateDBWithTestEntityPK(numItems = 5)

        block {
            subscription = TestEntityPK().queryAsObservable { query -> query.equalTo("id", 1) }.subscribe({
                assertThat(it).hasSize(1)
                assertThat(it[0].isManaged).isFalse()
                release()
            })
        }

        subscription?.unsubscribe()
    }

    /**
     * SINGLE AND FLOWABLE TESTS
     */

    @Test fun testQueryAllAsFlowable() {

        var itemsCount = 5
        var disposable: Disposable? = null

        populateDBWithTestEntity(numItems = itemsCount)

        block {
            disposable = TestEntity().queryAllAsFlowable().subscribe({
                assertThat(it).hasSize(itemsCount)
                release()
            })
        }

        block {
            //Add one item more to db
            ++itemsCount
            populateDBWithTestEntity(numItems = 1)
        }

        disposable?.dispose()

    }

    @Test fun testQueryAsFlowable() {

        populateDBWithTestEntityPK(numItems = 5)

        block {
            disposable = TestEntityPK().queryAsFlowable { query -> query.equalTo("id", 1) }.subscribe({
                assertThat(it).hasSize(1)
                assertThat(it[0].isManaged).isFalse()
                release()
            })
        }

        disposable?.dispose()
    }

     @Test fun testQueryAllAsSingle() {

         var itemsCount = 5

         populateDBWithTestEntity(numItems = itemsCount)

         block {
             disposable = TestEntity().queryAllAsSingle().subscribe({ result ->
                 assertThat(result).hasSize(itemsCount)
                 assertThat(result[0].isManaged).isFalse()
                 release()
             })

         }

         assertThat(disposable?.isDisposed ?: false).isTrue()
     }

     @Test fun testQueryAsSingle() {

         populateDBWithTestEntityPK(numItems = 5)

         block {
             disposable = TestEntityPK().queryAsSingle { query -> query.equalTo("id", 1) }.subscribe({ it ->
                 assertThat(it).hasSize(1)
                 assertThat(it[0].isManaged).isFalse()
                 release()
             })
         }

         assertThat(disposable?.isDisposed ?: false).isTrue()
     }

    /**
     * UTILITY TEST METHODS
     */
    private fun populateDBWithTestEntity(numItems: Int) {
        (0..numItems - 1).forEach { TestEntity().save() }
    }

    private fun populateDBWithTestEntityPK(numItems: Int) {
        (0..numItems - 1).forEach { TestEntityPK(it.toLong()).save() }
    }

    private fun blockLatch() {
        if (!latchReleased) {
            latch.await()
        }
    }

    private fun release() {
        latchReleased = true
        latch.countDown()
        latch = CountDownLatch(1)
    }


    fun block(closure: () -> Unit) {
        latchReleased = false
        closure()
        blockLatch()
    }


}


