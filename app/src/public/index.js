const upload = document.getElementById("upload");
const fileInput = document.querySelector('input[type="file"]');

async function renderFileList() {
    let list = document.getElementById("file-list")

    await fetch('/list')
        .then(r => r.json())
        .then(j => {
            console.log(j);
            list.innerHTML = "";
            j.forEach(f => {
                list.innerHTML = `<li>${f.name} <a href="${f.downloadURL}" download>
                    <button>Download</button></a></li> ${list.innerHTML}`
            });
        })
        .catch(r => list.innerHTML = `<p>⚠️ Error fetching files -> ${r}</p>`);
}

upload.addEventListener('click', async (e) => {
    for (const file of fileInput.files) {
        await uploadFile(file);
    }
})

async function uploadFile(file) {
    let data = new FormData();
    data.append('file',file,file.name);
    await fetch('/upload', {
        method: 'POST',
        body: data
    }).then(async r => {
        if (r.status == 200) {
            alert(`File ${file.name} uploaded successfully`)
            await renderFileList()
        } else {
            console.error(r.json())
        }
    });

}

await renderFileList()  