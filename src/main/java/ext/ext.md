- bookmark如何切换分支保留的
  - 经过测试，猜测每个分支一个bookmark文件，切换分支时，会自动切换到对应的bookmark文件
  - 之所以看起来有保留，比如不勾选idea的restore重制，那么缓存中有上一个分支的标签，所以会展示，其关闭后会存储到当前分支的bookmark文件
  - 有的已经删除，但是ui缓存没有更新
  - 搭配 Restore workspace 取消勾选，效果更佳。