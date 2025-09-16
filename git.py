from flask import Flask, request, jsonify, render_template

# tell Flask: templates are in current folder (.)
app = Flask(__name__, template_folder=".")

# In-memory store
todos = []
next_id = 1

@app.route('/')
def home():
    return render_template("ex1.html", result="Welcome to the To-Do App 🚀")

@app.route('/todos', methods=['GET'])
def get_todos():
    return jsonify(todos)

@app.route('/todos/<int:todo_id>', methods=['GET'])
def get_todo(todo_id):
    for t in todos:
        if t["id"] == todo_id:
            return jsonify(t)
    return jsonify({"error": "Not found"}), 404

@app.route('/todos', methods=['POST'])
def create_todo():
    global next_id
    data = request.get_json()
    todo = {
        "id": next_id,
        "title": data.get("title"),
        "done": False
    }
    todos.append(todo)
    next_id += 1
    return jsonify(todo), 201

@app.route('/todos/<int:todo_id>', methods=['PUT'])
def update_todo(todo_id):
    data = request.get_json()
    for t in todos:
        if t["id"] == todo_id:
            t["title"] = data.get("title", t["title"])
            t["done"] = data.get("done", t["done"])
            return jsonify(t)
    return jsonify({"error": "Not found"}), 404

@app.route('/todos/<int:todo_id>', methods=['DELETE'])
def delete_todo(todo_id):
    global todos
    todos = [t for t in todos if t["id"] != todo_id]
    return jsonify({"message": "Deleted"})

if __name__ == '__main__':
    app.run(host="0.0.0.0", port=5000, debug=True)