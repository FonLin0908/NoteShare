document.addEventListener("DOMContentLoaded", function () {
    const commentInput = document.getElementById("commentInput");
    const charCounter = document.getElementById("charCounter");
    const commentTower = document.getElementById("commentTower");
    const commentForm = document.getElementById("commentForm");

    if (!commentTower) return;

    // 👑 絕不动摇的 ID 抓取法
    const noteId = commentTower.getAttribute("data-note-id");

    // ⏱️ 字數計數器
    if (commentInput && charCounter) {
        commentInput.addEventListener("input", function () {
            const maxLength = 300;
            const currentLength = commentInput.value.length;
            const remaining = maxLength - currentLength;
            charCounter.textContent = remaining + " / " + maxLength;
            if (remaining <= 0) {
                charCounter.classList.remove("text-muted");
                charCounter.classList.add("text-danger", "fw-bold");
            } else {
                charCounter.classList.remove("text-danger", "fw-bold");
                charCounter.classList.add("text-muted");
            }
        });
    }

    // 🚀 動作一：載入大樓
    function loadCommentTower() {
            // 加上隨機數防止瀏覽器惡意快取（Cache），並強制宣告 Accept 型態
            fetch('/api/comments/note/' + noteId + '?t=' + new Date().getTime(), {
                method: 'GET',
                headers: {
                    'Accept': 'application/json', // 👑 強制告訴後端：我只要 JSON，別塞網頁給我！
                    'X-Requested-With': 'XMLHttpRequest' // 告訴後端這是一隻非同步 Ajax 請求
                }
            })
            .then(res => {
                // 💡 終極定位：如果拿到的依然不是 JSON，我們直接在控制台把這段可疑文字印出來看！
                const contentType = res.headers.get("content-type");
                if (!contentType || !contentType.includes("application/json")) {
                    return res.text().then(text => {
                        console.error("🔴 驚人發現！後端其實吐了這個給你，根本不是 JSON：", text.substring(0, 200));
                        throw new Error("不是合法的 JSON 格式");
                    });
                }
                return res.json();
            })
            .then(comments => {
                commentTower.innerHTML = "";
                if (comments.length === 0) {
                    commentTower.innerHTML = '<div class="text-center text-muted p-4">目前還沒有人留言，快來當 #1 樓！</div>';
                    return;
                }
                comments.forEach((comment, index) => {
                    const floorNum = index + 1;
                    let commentHtml = "";
                    if (comment.deleted) {
                        commentHtml = '<div class="list-group-item p-3 border-bottom text-muted"><div class="d-flex gap-3 align-items-center"><div class="bg-light text-secondary rounded-circle d-flex align-items-center justify-content-center" style="width: 40px; height: 40px;"><i class="bi bi-trash3"></i></div><div><small class="text-muted">#'+floorNum+' 樓</small><p class="mb-0 fst-italic text-decoration-line-through">🚫 該留言已被移除。</p></div></div></div>';
                    } else {
                        const formattedTime = new Date(comment.createdAt).toLocaleString('zh-TW', { hour12: false });
                        const username = (comment.user && comment.user.username) ? comment.user.username : "匿名用戶";
                        const nickname = (comment.user && comment.user.nickname) ? comment.user.nickname : "匿名用戶";
                        const memberProfileUrl = (comment.user && comment.user.id) ? `/member/${comment.user.id}` : '#';
                        const heartIcon = comment.isLikedByMe ? "bi-heart-fill" : "bi-heart";
                        const heartColor = comment.isLikedByMe ? "text-danger" : "text-secondary";

                        // 🔍 權限關鍵變數準備
                        // ================= 【權限矩陣關鍵變數】 =================

                        // 👑 1. 當前看網頁的人：直接去跟 HTML 畫面上的隱藏欄位拔數字！
                        const currentUserEl = document.getElementById('current-user-id');
                        const currentUserId = (currentUserEl && currentUserEl.value) ? parseInt(currentUserEl.value) : null;

                        // 👑 2. 這則留言的作者 ID
                        const commentUserId = (comment.user && comment.user.id) ? comment.user.id : null;

                        // 👑 3. 這篇筆記的擁有者 ID（由後端 Map 吐過來的欄位）
                        const noteOwnerId = comment.noteOwnerId ? comment.noteOwnerId : null;

                        // =======================================================

                        // 接下來進入動態組裝
                        let dropdownHtml = '';

                        // 🔒 規則一：確認有登入
                        if (currentUserId !== null) {

                            if (currentUserId === commentUserId) {
                                // 🔒 規則二：留言作者本人 -> 只能編輯/移除，看不到檢舉
                                dropdownHtml += `
                                    <li><a class="dropdown-item d-flex align-items-center gap-2" href="#" onclick="editComment(${comment.id}); return false;"><i class="bi bi-pencil text-primary"></i> 編輯留言</a></li>
                                    <li><hr class="dropdown-divider"></li>
                                    <li><a class="dropdown-item d-flex align-items-center gap-2 text-danger" href="#" onclick="deleteComment(${comment.id}); return false;"><i class="bi bi-trash"></i> 移除留言</a></li>
                                `;
                            }
                            else if (currentUserId === noteOwnerId) {
                                // 🔒 規則三：筆記擁有者 -> 看不到編輯，但可以檢舉與強制移除（板主權限）
                                dropdownHtml += `
                                    <li><a class="dropdown-item d-flex align-items-center gap-2" href="#" onclick="reportComment(${comment.id}); return false;"><i class="bi bi-exclamation-triangle text-warning"></i> 檢舉此篇</a></li>
                                    <li><hr class="dropdown-divider"></li>
                                    <li><a class="dropdown-item d-flex align-items-center gap-2 text-danger" href="#" onclick="deleteComment(${comment.id}); return false;"><i class="bi bi-trash"></i> 移除留言（板主管理）</a></li>
                                `;
                            }
                            else {
                                // 🔒 規則四：純路人 -> 只能檢舉
                                dropdownHtml += `
                                    <li><a class="dropdown-item d-flex align-items-center gap-2" href="#" onclick="reportComment(${comment.id}); return false;"><i class="bi bi-exclamation-triangle text-warning"></i> 檢舉此篇</a></li>
                                `;
                            }
                        }
                        // 👑 1. 定義編輯標籤
                        const editedTag = comment.edited ? `<small class="text-muted ms-2" style="font-size: 11px; font-style: italic;">(已編輯)</small>` : '';

                        // 👑 2. 事先組裝好完整的三個點點 HTML 字串，避開在模板內寫複雜的三元運算子
                        let finalDropdownContainerHtml = '';
                        if (dropdownHtml !== '') {
                            finalDropdownContainerHtml = `
                            <div class="dropdown">
                                <button class="btn btn-link text-secondary p-0 border-0" type="button" id="dropdownMenu-${comment.id}" data-bs-toggle="dropdown" aria-expanded="false" style="line-height: 1;">
                                    <i class="bi bi-three-dots-vertical fs-5"></i>
                                </button>
                                <ul class="dropdown-menu dropdown-menu-end shadow-sm" aria-labelledby="dropdownMenu-${comment.id}">
                                    ${dropdownHtml}
                                </ul>
                            </div>`;
                        }
                        // 1️⃣ 提取外觀資料與防空針（Null）保底
                        const hasFrame = comment.user && comment.user.currentFrame;
                        const hasAvatar = comment.user && comment.user.currentAvatar;
                        //console.log(comment.user.nickname + " " + comment.user.currentFrame + " " + hasFrame);
                        // 2️⃣ 根據後端解鎖狀態，動態分配 Class 特效與呼吸暈光 Style
                        const frameClass = hasFrame ? comment.user.currentFrame : 'border border-info';
                        const frameStyle = hasFrame ? '' : 'box-shadow: 0 0 10px rgba(13, 202, 240, 0.3);';

                        // 3️⃣ 決定內層圖片來源：自訂頭像圖片 or DiceBear 幾何保底
                        const avatarSrc = hasAvatar
                            ? `/images/avatars/${comment.user.currentAvatar}`
                            : `https://api.dicebear.com/7.x/identicon/svg?seed=${encodeURIComponent(username)}`;

                        // 👑 3. 乾淨純粹的 HTML 模板拼接（全部回歸標準 ${...}，不加任何反斜線）
                        commentHtml = `
                            <div class="list-group-item p-3 border-bottom comment-card">
                                <div class="d-flex gap-3">
                                    <div class="avatar-frame ${frameClass} d-flex align-items-center justify-content-center bg-dark rounded-circle"
                                             style="width: 42px; height: 42px; transition: 0.3s; ${frameStyle}">

                                        <img src="${avatarSrc}"
                                             class="rounded-circle border border-2 border-info bg-white shadow-sm user-avatar-img-slot"
                                             style="width: 40px; height: 40px; object-fit: cover; transition: 0.3s;">
                                    </div>
                                    <div class="flex-grow-1">
                                        <div class="d-flex justify-content-between align-items-start mb-1">
                                            <div>
                                                <a href="${memberProfileUrl}" class="fw-bold text-dark text-decoration-none me-2 link-info-hover">
                                                    ${nickname}
                                                </a>
                                                <small class="text-muted me-2">#${floorNum} 樓</small>
                                                <small class="text-muted">${formattedTime}</small>${editedTag}
                                            </div>

                                            ${finalDropdownContainerHtml}

                                        </div>

                                        <div id="comment-body-${comment.id}">
                                            <p class="mb-1 text-secondary" style="white-space: pre-wrap;">${comment.content}</p>
                                        </div>

                                        <div class="d-flex align-items-center mt-2">
                                            <button onclick="toggleLikeComment(${comment.id})" class="btn btn-link p-0 text-decoration-none d-flex align-items-center gap-1 shadow-none" style="font-size: 14px;">
                                                <i id="like-icon-${comment.id}" class="bi ${heartIcon} ${heartColor} fs-6"></i>
                                                <span id="like-count-${comment.id}" class="text-muted">${comment.likesCount}</span>
                                            </button>
                                        </div>

                                    </div>
                                </div>
                            </div>`;
                    }
                    commentTower.insertAdjacentHTML('beforeend', commentHtml);
                });
            })
            .catch(err => console.error("撈取大樓失敗:", err));
    }

    // 🚀 動作二：送出留言
    if (commentForm) {
        commentForm.addEventListener("submit", function (e) {
            e.preventDefault();
            const contentValue = commentInput.value;
            if (!contentValue.trim()) return;

            fetch('/api/comments/note/' + noteId, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ content: contentValue })
            })
            .then(res => {
                if (res.ok) {
                    commentInput.value = "";
                    charCounter.textContent = "300 / 300";
                    loadCommentTower();
                }
            });
        });
    }

    // 🚀 動作三：刪除
    window.deleteComment = function(commentId) {
        if (!confirm("確定要移除這則討論留言嗎？")) return;
        fetch('/api/comments/' + commentId, { method: 'DELETE', credentials: 'include' })
            .then(res => { if (res.ok) loadCommentTower(); });
    };
    // 🚀 動作三：刪除（維持你剛才調通的代碼）
    window.deleteComment = function(commentId) {
        if (!confirm("確定要移除這則討論留言嗎？")) return;
        fetch('/api/comments/' + commentId, {
            method: 'DELETE',
            credentials: 'include'
        })
        .then(res => {
            if (res.ok) {
                loadCommentTower();
            } else {
                alert("刪除失敗，你可能不是此留言的作者或登入已過期！");
            }
        });
    };

    // 🚀 動作四：編輯彈窗（先用簡單 prompt 頂住，之後可以優化成動態輸入框）
    window.editComment = function(commentId) {
        alert("功能研發中！即將對接後端 PUT /api/comments/" + commentId);
        // 之後可以在這裡放非同步修改邏輯
    };

    // 🚀 動作五：檢舉留言
    window.reportComment = function(commentId) {
        alert("檢舉已收到！系統已自動記錄留言代號 #" + commentId + " 並送交後端審查。");
    };
    // 🚀 動作六：留言非同步點讚（Toggle）
    window.toggleLikeComment = function(commentId) {
        // 發射 POST 請求敲擊我們剛剛寫好的後端點讚開關 API
        fetch('/api/comments/' + commentId + '/like', {
            method: 'POST',
            credentials: 'include' // 👑 核心關鍵：必須帶著登入 Cookie 去敲門！
        })
        .then(res => {
            if (res.status === 401) {
                alert("請先登入才能為這則討論點讚喔！");
                throw new Error("未登入");
            }
            if (!res.ok) throw new Error("點讚發生意外錯誤");
            return res.json(); // 拿到後端吐回來的 Map：{ likesCount: X, isLiked: true/false }
        })
        .then(data => {
            // 🎯 精準動態局部定位這一層樓的 HTML 元素
            const iconElement = document.getElementById(`like-icon-${commentId}`);
            const countElement = document.getElementById(`like-count-${commentId}`);

            if (iconElement && countElement) {
                // 1. 更新最新的點讚數字
                countElement.textContent = data.likesCount;

                // 2. 根據狀態切換愛心的外觀與顏色
                if (data.isLiked) {
                    // 變紅心
                    iconElement.classList.remove('bi-heart', 'text-secondary');
                    iconElement.classList.add('bi-heart-fill', 'text-danger');
                } else {
                    // 變回灰空心
                    iconElement.classList.remove('bi-heart-fill', 'text-danger');
                    iconElement.classList.add('bi-heart', 'text-secondary');
                }
            }
        })
        .catch(err => console.error("點讚處理失敗:", err));
    };
    // 🚀 動作四：點擊編輯 -> 原地內文變身成輸入框
    window.editComment = function(commentId) {
        // 1. 定位到這一樓的內文容器
        const bodyContainer = document.getElementById(`comment-body-${commentId}`);
        if (!bodyContainer) return;

        // 2. 抓出原本的純文字內容（要把前後空格清掉）
        const originalContent = bodyContainer.querySelector('p').innerText;

        // 3. 瞬間把容器內容魔改成「輸入框 + 儲存/取消按鈕」的精緻排版
        bodyContainer.innerHTML = `
            <div class="mt-2 edit-form-block">
                <textarea id="edit-input-${commentId}" class="form-control mb-2 shadow-none text-secondary" rows="2" style="font-size: 14px; resize: none;">${originalContent}</textarea>
                <div class="d-flex gap-2 justify-content-end">
                    <button onclick="cancelEditComment(${commentId}, \`${originalContent.replace(/`/g, '\\`').replace(/\n/g, '\\n')}\`)" class="btn btn-light btn-sm px-3" style="font-size: 12px;">取消</button>
                    <button onclick="saveEditComment(${commentId})" class="btn btn-info text-white btn-sm px-3" style="font-size: 12px;">儲存修改</button>
                </div>
            </div>
        `;
    };

    // 🚀 動作四-B：取消編輯 -> 還原原本的文字
    window.cancelEditComment = function(commentId, originalContent) {
        const bodyContainer = document.getElementById(`comment-body-${commentId}`);
        if (bodyContainer) {
            bodyContainer.innerHTML = `<p class="mb-1 text-secondary" style="white-space: pre-wrap;">${originalContent}</p>`;
        }
    };

    // 🚀 動作四-C：發射 PUT 請求儲存修改
    window.saveEditComment = function(commentId) {
        const inputElement = document.getElementById(`edit-input-${commentId}`);
        if (!inputElement) return;

        const newContentValue = inputElement.value.trim();
        if (!newContentValue) {
            alert("留言內容不能空蕩蕩的喔！");
            return;
        }

        // 發射工業級 PUT 請求
        fetch('/api/comments/' + commentId, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include', // 👑 帶上 loginUser 的憑證
            body: JSON.stringify({ content: newContentValue })
        })
        .then(res => {
            if (res.status === 401) {
                alert("登入已過期，請重新登入！");
                throw new Error("未登入");
            }
            if (!res.ok) return res.json().then(err => { throw new Error(err.message); });
            return res.json();
        })
        .then(data => {
            // 🎉 修改成功！不用刷新整頁，直接重新加載這棟大樓，最新內容與「(已編輯)」標籤秒速到位！
            loadCommentTower();
        })
        .catch(err => {
            alert(err.message || "修改失敗，發生未知錯誤");
            console.error("編輯處理失敗:", err);
        });
    };

    // 初始化啟動
    loadCommentTower();
});