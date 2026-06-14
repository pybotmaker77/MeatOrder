package com.example.meatorder.data.dao

import androidx.room.*
import com.example.meatorder.data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM entities ORDER BY entity")
    fun getAllEntities(): Flow<List<MeatEntity>>

    @Query("SELECT * FROM entities WHERE id = :id")
    suspend fun getEntityById(id: Int): MeatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntities(entities: List<MeatEntity>)

    @Insert
    suspend fun insertEntity(entity: MeatEntity)

    @Update
    suspend fun updateEntity(entity: MeatEntity)

    @Delete
    suspend fun deleteEntity(entity: MeatEntity)

    @Query("DELETE FROM entities")
    suspend fun deleteAllEntities()

    @Query("SELECT * FROM templates ORDER BY temp")
    fun getAllTemplates(): Flow<List<Template>>

    @Query("SELECT * FROM templates WHERE id = :id")
    suspend fun getTemplateById(id: Int): Template?

    @Insert
    suspend fun insertTemplate(template: Template): Long

    @Update
    suspend fun updateTemplate(template: Template)

    @Delete
    suspend fun deleteTemplate(template: Template)

    @Query("DELETE FROM templates")
    suspend fun deleteAllTemplates()

    @Query("SELECT * FROM template_items WHERE template_id = :templateId")
    fun getTemplateItems(templateId: Int): Flow<List<TemplateItem>>

    @Query("DELETE FROM template_items WHERE template_id = :templateId")
    suspend fun deleteTemplateItemsByTemplateId(templateId: Int)

    @Insert
    suspend fun insertTemplateItem(item: TemplateItem)

    @Update
    suspend fun updateTemplateItem(item: TemplateItem)

    @Delete
    suspend fun deleteTemplateItem(item: TemplateItem)

    @Query("DELETE FROM template_items")
    suspend fun deleteAllTemplateItems()

    @Query("SELECT * FROM patterns")
    fun getAllPatterns(): Flow<List<Pattern>>

    @Insert
    suspend fun insertPattern(pattern: Pattern)

    @Update
    suspend fun updatePattern(pattern: Pattern)

    @Delete
    suspend fun deletePattern(pattern: Pattern)

    @Query("UPDATE patterns SET is_active = 0")
    suspend fun deactivateAllPatterns()

    @Query("UPDATE patterns SET is_active = 1 WHERE id = :id")
    suspend fun activatePattern(id: Int)

    @Query("SELECT * FROM input_types ORDER BY type_name")
    fun getAllInputTypes(): Flow<List<InputType>>

    @Query("SELECT * FROM input_types WHERE type_name = :typeName LIMIT 1")
    suspend fun getInputTypeByName(typeName: String): InputType?

    @Insert
    suspend fun insertInputType(inputType: InputType)

    @Update
    suspend fun updateInputType(inputType: InputType)

    @Delete
    suspend fun deleteInputType(inputType: InputType)

    @Query("DELETE FROM input_types")
    suspend fun deleteAllInputTypes()

    // Минимальный заказ
    @Query("SELECT * FROM min_order_items")
    fun getAllMinOrderItems(): Flow<List<MinOrderItem>>

    @Insert
    suspend fun insertMinOrderItem(item: MinOrderItem)

    @Update
    suspend fun updateMinOrderItem(item: MinOrderItem)

    @Delete
    suspend fun deleteMinOrderItem(item: MinOrderItem)

    @Query("DELETE FROM min_order_items")
    suspend fun deleteAllMinOrderItems()
}
