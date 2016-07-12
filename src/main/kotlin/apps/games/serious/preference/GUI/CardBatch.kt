package apps.games.serious.preference.GUI

import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.g3d.RenderableProvider
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.ObjectSet
import com.badlogic.gdx.utils.Pool


/**
 * Created by user on 7/1/16.
 */

/**
 * Class for rendering cards
 */
class CardBatch(mainMaterial: Material, selectionMaterial: Material) : ObjectSet<Card>(), RenderableProvider, Disposable {
    val renderable = Renderable()
    val normalMesh: Mesh
    val selectedMesh: Mesh
    val meshBuilder = MeshBuilder()
    val selectedRenderable = Renderable()

    init {
        val maxNumberOfCards = 52
        val maxNumberOfVertices = maxNumberOfCards * 8
        val maxNumberOfIndices = maxNumberOfCards * 12
        normalMesh = Mesh(false, maxNumberOfVertices, maxNumberOfIndices,
                VertexAttribute.Position(), VertexAttribute.Normal(),
                VertexAttribute.TexCoords(0))
        selectedMesh = Mesh(false, maxNumberOfVertices, maxNumberOfIndices,
                VertexAttribute.Position(), VertexAttribute.Normal(),
                VertexAttribute.TexCoords(0))

        renderable.material = mainMaterial
        selectedRenderable.material = selectionMaterial
    }

    override fun getRenderables(renderables: Array<Renderable>,
            pool: Pool<Renderable>?) {
        meshBuilder.begin(normalMesh.vertexAttributes)
        meshBuilder.part("cards", GL20.GL_TRIANGLES, renderable.meshPart)
        for (card in this) {
            meshBuilder.setVertexTransform(card.transform)
            meshBuilder.addMesh(card.verticies, card.indices)
        }
        meshBuilder.end(normalMesh)
        renderables.add(renderable)
        if (this.any { x -> x.isSelected }) {
            meshBuilder.begin(selectedMesh.vertexAttributes)
            meshBuilder.part("selected", GL20.GL_TRIANGLES,
                    selectedRenderable.meshPart)
            for (selected in this.filter { x -> x.isSelected }) {
                meshBuilder.setVertexTransform(selected.transform)
                meshBuilder.addMesh(selected.verticies, selected.indices)
            }
            meshBuilder.end(selectedMesh)
            renderables.add(selectedRenderable)
        }

    }

    override fun dispose() {
        normalMesh.dispose()
    }
}
