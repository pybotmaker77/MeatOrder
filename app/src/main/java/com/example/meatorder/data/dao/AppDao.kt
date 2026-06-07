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

    @Delete
    suspend fun deleteEntity(entity: MeatEntity)

    @Query("SELECT * FROM templates ORDER BY temp")
    fun getAllTemplates(): Flow<List<Template>>

    @Query("SELECT * FROM templates WHERE id = :id")
    suspend fun getTemplateById(id: Int): Template?

    @Insert
    suspend fun insertTemplate(template: Template): Long

    @Delete
    suspend fun deleteTemplate(template: Template)

    @Query("SELECT * FROM template_items WHERE template_id = :templateId")
    fun getTemplateItems(templateId: Int): Flow<List<TemplateItem>>

    @Insert
    suspend fun insertTemplateItem(item: TemplateItem)

    @Update
    suspend fun updateTemplateItem(item: TemplateItem)

    @Delete
    suspend fun deleteTemplateItem(item: TemplateItem)

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
}
